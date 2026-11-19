package com.yea.sap.ccv2.devops.mcp.server.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;

@Component
public class SapCcv2TokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(SapCcv2TokenProvider.class);
    private static final String SAP_PORTAL_API_TOKEN_URL = "https://portalapi.commerce.ondemand.com/hybris/oauth2/v1/token";
    private static final long DEFAULT_EXPIRES_IN_SECONDS = 300L;
    private static final long EXPIRY_MARGIN_SECONDS = 60L;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String clientSecret;
    private final String tokenEndpoint;
    private final String resource;

    private String cachedAccessToken;
    private Instant cachedTokenExpiresAt;

    public SapCcv2TokenProvider(RestTemplate restTemplate,
                                ObjectMapper objectMapper,
                                @Value("${sap.ccv2.api.clientId}") String clientId,
                                @Value("${sap.ccv2.api.clientSecret}") String clientSecret,
                                @Value("${sap.ccv2.api.tokenEndpoint}") String tokenEndpoint,
                                @Value("${sap.ccv2.api.resource}") String resource) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenEndpoint = tokenEndpoint;
        this.resource = resource;
    }

    public synchronized String getAccessToken() {
        if (cachedAccessToken != null && cachedTokenExpiresAt != null && Instant.now().isBefore(cachedTokenExpiresAt)) {
            return cachedAccessToken;
        }

        validateOauthConfiguration();

        try {
            return cacheAndReturnToken(requestAccessToken(tokenEndpoint));
        } catch (ResourceAccessException e) {
            if (!isApiYaasDnsFailure(e, tokenEndpoint)) {
                throw buildTokenEndpointAccessException(tokenEndpoint, e);
            }
            logger.warn("Token endpoint host is unreachable ({}). Retrying with fallback endpoint: {}", tokenEndpoint, SAP_PORTAL_API_TOKEN_URL);
            try {
                return cacheAndReturnToken(requestAccessToken(SAP_PORTAL_API_TOKEN_URL));
            } catch (ResourceAccessException fallbackError) {
                throw buildTokenEndpointAccessException(SAP_PORTAL_API_TOKEN_URL, fallbackError);
            }
        }
    }

    private OAuthTokenResponse requestAccessToken(String endpoint) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "client_credentials");
        body.add("resource", resource);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);
            return parseTokenResponse(response.getBody());
        } catch (RestClientResponseException e) {
            logger.error("Error retrieving SAP CCv2 token from {}. Status: {}, body: {}", endpoint, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to retrieve SAP CCv2 access token from " + endpoint + ". Status: " + e.getStatusCode(), e);
        }
    }

    private OAuthTokenResponse parseTokenResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new RuntimeException("SAP CCv2 token endpoint returned an empty response.");
        }
        try {
            return objectMapper.readValue(responseBody, OAuthTokenResponse.class);
        } catch (JsonProcessingException e) {
            logger.error("Non-JSON token response from SAP CCv2. Body: {}", responseBody);
            throw new RuntimeException("SAP CCv2 token endpoint returned a non-JSON response.", e);
        }
    }

    private String cacheAndReturnToken(OAuthTokenResponse tokenResponse) {
        if (tokenResponse.accessToken == null || tokenResponse.accessToken.isBlank()) {
            throw new RuntimeException("SAP CCv2 token endpoint returned an empty access token.");
        }

        long expiresIn = tokenResponse.expiresIn != null ? tokenResponse.expiresIn : DEFAULT_EXPIRES_IN_SECONDS;
        long effectiveLifetime = Math.max(1L, expiresIn - EXPIRY_MARGIN_SECONDS);

        cachedAccessToken = tokenResponse.accessToken;
        cachedTokenExpiresAt = Instant.now().plusSeconds(effectiveLifetime);
        logger.debug("Successfully generated SAP CCv2 access token");
        return cachedAccessToken;
    }

    private boolean isApiYaasDnsFailure(ResourceAccessException e, String endpoint) {
        URI uri = URI.create(endpoint);
        if (!"api.yaas.io".equalsIgnoreCase(uri.getHost())) {
            return false;
        }
        return e.getCause() instanceof UnknownHostException;
    }

    private RuntimeException buildTokenEndpointAccessException(String endpoint, ResourceAccessException e) {
        logger.error("Unable to access SAP CCv2 token endpoint {}: {}", endpoint, e.getMessage());
        return new RuntimeException(
            "Cannot reach SAP CCv2 token endpoint: " + endpoint
                + ". If this endpoint uses api.yaas.io, switch to " + SAP_PORTAL_API_TOKEN_URL + ".",
            e
        );
    }

    private void validateOauthConfiguration() {
        requireConfigured(clientId, "clientId");
        requireConfigured(clientSecret, "clientSecret");
        requireConfigured(tokenEndpoint, "tokenEndpoint");
        requireConfigured(resource, "resource");
    }

    private void requireConfigured(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SAP CCv2 " + property + " is not configured. Please set sap.ccv2.api." + property + ".");
        }
    }

    private static class OAuthTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expires_in")
        private Long expiresIn;
    }
}
