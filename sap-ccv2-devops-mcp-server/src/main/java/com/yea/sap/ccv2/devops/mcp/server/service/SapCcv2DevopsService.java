package com.yea.sap.ccv2.devops.mcp.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yea.sap.ccv2.devops.mcp.server.model.BuildProgress;
import com.yea.sap.ccv2.devops.mcp.server.model.BuildResponse;
import com.yea.sap.ccv2.devops.mcp.server.model.BuildsResponse;
import com.yea.sap.ccv2.devops.mcp.server.model.CreateBuildRequest;
import com.yea.sap.ccv2.devops.mcp.server.model.CreateDeploymentRequest;
import com.yea.sap.ccv2.devops.mcp.server.model.DeploymentResponse;
import com.yea.sap.ccv2.devops.mcp.server.model.DeploymentsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownContentTypeException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.zip.GZIPInputStream;


@Service
public class SapCcv2DevopsService
{
    private static final Logger logger = LoggerFactory.getLogger(SapCcv2DevopsService.class);
    
    @Value("${sap.ccv2.api.subscriptionCode}")
    private String subscriptionCode;

    @Value("${sap.ccv2.api.clientId}")
    private String clientId;

    @Value("${sap.ccv2.api.clientSecret}")
    private String clientSecret;

    @Value("${sap.ccv2.api.tokenEndpoint}")
    private String tokenEndpoint;

    @Value("${sap.ccv2.api.resource}")
    private String resource;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String SAP_PORTAL_API_BUILD_URL = "https://portalapi.commerce.ondemand.com/v2/subscriptions/{subscriptionCode}/builds";
    private static final String SAP_PORTAL_API_DEPLOY_URL = "https://portalapi.commerce.ondemand.com/v2/subscriptions/{subscriptionCode}/deployments";
    private static final String SAP_PORTAL_API_TOKEN_URL = "https://portalapi.commerce.ondemand.com/hybris/oauth2/v1/token";
    private volatile String cachedAccessToken;
    private volatile Instant cachedTokenExpiresAt;

    public SapCcv2DevopsService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private HttpHeaders getHeaders() {
        String accessToken = getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("x-approuter-authorization", "Bearer " + accessToken);
        return headers;
    }

    public BuildResponse createBuild(CreateBuildRequest request) {
        String url = SAP_PORTAL_API_BUILD_URL.replace("{subscriptionCode}", subscriptionCode);
        
        logger.info("createBuild request: subscriptionCode={}, branch={}, name={}",
                    subscriptionCode, request.getBranch(), request.getName());

        HttpEntity<CreateBuildRequest> entity = new HttpEntity<>(request, getHeaders());

        ResponseEntity<byte[]> response = restTemplate.postForEntity(url, entity, byte[].class);
        String rawBody = decodeResponseBody(response.getBody());
        logger.info("createBuild response: status={}, contentType={}, body={}", response.getStatusCode(),
                    response.getHeaders().getContentType(), rawBody);
        try {
            return objectMapper.readValue(rawBody, BuildResponse.class);
        } catch (JsonProcessingException e) {
            logger.warn("createBuild: response body is not the expected JSON, build was accepted with status {}", response.getStatusCode());
            return new BuildResponse();
        }
    }

    private String decodeResponseBody(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        boolean gzipped = bytes.length >= 2 && (bytes[0] & 0xff) == 0x1f && (bytes[1] & 0xff) == 0x8b;
        if (!gzipped) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to decompress SAP CCv2 response", e);
        }
    }

    public BuildsResponse getBuilds(Integer top) {
        String baseUrl = SAP_PORTAL_API_BUILD_URL.replace("{subscriptionCode}", subscriptionCode);
        
        // Hardcoded default values
        Integer skip = 0;
        String orderby = "buildStartTimestamp desc";
        Boolean count = true;
        String statusNot = "DELETED";
        if (top == null) {
            top = 5;
        }

        logger.info("getBuilds request: subscriptionCode={}, top={}, skip={}, orderby={}, count={}, statusNot={}", 
                    subscriptionCode, top, skip, orderby, count, statusNot);

        StringBuilder queryString = new StringBuilder();
        queryString.append("?$top=").append(top);
        queryString.append("&$skip=").append(skip);
        queryString.append("&statusNot=").append(statusNot);
        queryString.append("&$count=").append(count);
        queryString.append("&$orderby=").append(orderby);
        
        String urlWithParams = baseUrl + queryString.toString();
        logger.debug("Request URL: {}", urlWithParams);
        
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(urlWithParams, HttpMethod.GET, entity, String.class);
            logger.info("Successfully retrieved builds. Response status: {}", response.getStatusCode());
            return readJsonBody(response.getBody(), BuildsResponse.class, "retrieving builds");
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("Authentication failed (401): {}", e.getMessage());
            throw new RuntimeException("Authentication failed. Please verify your SAP CCv2 credentials and token.", e);
        } catch (RestClientResponseException e) {
            logger.error("HTTP error when retrieving builds. Status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("SAP CCv2 request failed while retrieving builds. Status: " + e.getStatusCode(), e);
        } catch (UnknownContentTypeException e) {
            logger.error("Unexpected content type when retrieving builds. Status: {}, contentType: {}, body: {}",
                e.getStatusCode(), e.getContentType(), e.getResponseBodyAsString());
            throw new RuntimeException("SAP CCv2 returned a non-JSON response while retrieving builds. Verify API token, subscription code, and endpoint access.", e);
        } catch (Exception e) {
            logger.error("Error retrieving builds: {}", e.getMessage(), e);
            throw e;
        }
    }

    public BuildProgress getBuildProgress(String buildCode) {
        String url = SAP_PORTAL_API_BUILD_URL.replace("{subscriptionCode}", subscriptionCode) + "/" + buildCode + "/progress";
        
        logger.info("getBuildProgress request: subscriptionCode={}, buildCode={}", subscriptionCode, buildCode);
        
        logger.debug("Request URL: {}", url);
        
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            logger.info("Successfully retrieved build progress. Response status: {}", response.getStatusCode());
            return readJsonBody(response.getBody(), BuildProgress.class, "retrieving build progress");
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("Authentication failed (401): {}", e.getMessage());
            throw new RuntimeException("Authentication failed. Please verify your SAP CCv2 credentials and token.", e);
        } catch (RestClientResponseException e) {
            logger.error("HTTP error when retrieving build progress. Status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("SAP CCv2 request failed while retrieving build progress. Status: " + e.getStatusCode(), e);
        } catch (UnknownContentTypeException e) {
            logger.error("Unexpected content type when retrieving build progress. Status: {}, contentType: {}, body: {}",
                e.getStatusCode(), e.getContentType(), e.getResponseBodyAsString());
            throw new RuntimeException("SAP CCv2 returned a non-JSON response while retrieving build progress. Verify API token, subscription code, and endpoint access.", e);
        } catch (Exception e) {
            logger.error("Error retrieving build progress: {}", e.getMessage(), e);
            throw e;
        }
    }

    public DeploymentResponse createDeployment(CreateDeploymentRequest request) {
        String url = SAP_PORTAL_API_DEPLOY_URL.replace("{subscriptionCode}", subscriptionCode);

        logger.info("createDeployment request: subscriptionCode={}, buildCode={}, environmentCode={}, databaseUpdateMode={}, strategy={}",
            subscriptionCode,
            request.getBuildCode(),
            request.getEnvironmentCode(),
            request.getDatabaseUpdateMode(),
            request.getStrategy());

        logger.debug("Request URL: {}", url);

        HttpEntity<CreateDeploymentRequest> entity = new HttpEntity<>(request, getHeaders());

        try {
            ResponseEntity<DeploymentResponse> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, DeploymentResponse.class);

            logger.info("Deployment created successfully. Response status: {}", response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("Authentication failed (401): {}", e.getMessage());
            throw new RuntimeException("Authentication failed. Please verify your SAP CCv2 credentials and token.", e);
        } catch (UnknownContentTypeException e) {
            logger.error("Unexpected content type when creating deployment. Status: {}, contentType: {}, body: {}",
                e.getStatusCode(), e.getContentType(), e.getResponseBodyAsString());
            throw new RuntimeException("SAP CCv2 returned a non-JSON response while creating deployment. Verify API token, subscription code, and endpoint access.", e);
        } catch (Exception e) {
            logger.error("Error creating deployment: {}", e.getMessage(), e);
            throw e;
        }
    }

    public DeploymentsResponse getDeployments(
        Integer top)
    {
        String baseUrl = SAP_PORTAL_API_DEPLOY_URL.replace("{subscriptionCode}", subscriptionCode);

        // Paramètres par défaut
        if (top == null) top = 5;
        Integer skip = 0;
        Boolean count = true;
        String orderby = "createdTimestamp desc";

        // Construction du query string
        StringBuilder queryString = new StringBuilder();
        queryString.append("?$top=").append(top);
        queryString.append("&$skip=").append(skip);
        queryString.append("&$count=").append(count);
        queryString.append("&$orderby=").append(orderby);


            String urlWithParams = baseUrl + queryString.toString();
            logger.info("getDeployments request: subscriptionCode={}, top={}, skip={}, orderby={}, count={}",
                subscriptionCode, top, skip, orderby, count);

            logger.debug("Request URL: {}", urlWithParams);

            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());

            try
            {
                ResponseEntity<String> response =
                    restTemplate.exchange(urlWithParams, HttpMethod.GET, entity, String.class);
                logger.info("Successfully retrieved deployments. Response status: {}", response.getStatusCode());
                return readJsonBody(response.getBody(), DeploymentsResponse.class, "retrieving deployments");
            }
            catch (HttpClientErrorException.Unauthorized e)
            {
                logger.error("Authentication failed (401): {}", e.getMessage());
                throw new RuntimeException("Authentication failed. Please verify your SAP CCv2 credentials and token.", e);
            }
            catch (RestClientResponseException e)
            {
                logger.error("HTTP error when retrieving deployments. Status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new RuntimeException("SAP CCv2 request failed while retrieving deployments. Status: " + e.getStatusCode(), e);
            }
            catch (UnknownContentTypeException e)
            {
                logger.error("Unexpected content type when retrieving deployments. Status: {}, contentType: {}, body: {}",
                    e.getStatusCode(), e.getContentType(), e.getResponseBodyAsString());
                throw new RuntimeException("SAP CCv2 returned a non-JSON response while retrieving deployments. Verify API token, subscription code, and endpoint access.", e);
            }
            catch (Exception e)
            {
                logger.error("Error retrieving deployments: {}", e.getMessage(), e);
                throw e;
            }
        }

    private <T> T readJsonBody(String responseBody, Class<T> responseType, String operation) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new RuntimeException("SAP CCv2 returned an empty response while " + operation + ".");
        }

        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (JsonProcessingException e) {
            logger.error("Non-JSON response while {}. Body: {}", operation, responseBody);
            throw new RuntimeException("SAP CCv2 returned a non-JSON response while " + operation + ".", e);
        }
    }

    private synchronized String getAccessToken() {
        if (cachedAccessToken != null && cachedTokenExpiresAt != null && Instant.now().isBefore(cachedTokenExpiresAt)) {
            return cachedAccessToken;
        }

        validateOauthConfiguration();

        String effectiveTokenEndpoint = tokenEndpoint;
        boolean shouldRetryWithPortalEndpoint = false;

        try {
            OAuthTokenResponse tokenResponse = requestAccessToken(effectiveTokenEndpoint);
            return cacheAndReturnToken(tokenResponse);
        } catch (ResourceAccessException e) {
            if (isApiYaasDnsFailure(e, effectiveTokenEndpoint)) {
                shouldRetryWithPortalEndpoint = true;
            } else {
                throw buildTokenEndpointAccessException(effectiveTokenEndpoint, e);
            }
        } catch (RestClientResponseException e) {
            logger.error("Error retrieving SAP CCv2 token. Status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to retrieve SAP CCv2 access token. Status: " + e.getStatusCode(), e);
        }

        if (shouldRetryWithPortalEndpoint) {
            logger.warn("Token endpoint host is unreachable ({}). Retrying with fallback endpoint: {}", tokenEndpoint, SAP_PORTAL_API_TOKEN_URL);
            OAuthTokenResponse tokenResponse;
            try {
                tokenResponse = requestAccessToken(SAP_PORTAL_API_TOKEN_URL);
            } catch (ResourceAccessException e) {
                throw buildTokenEndpointAccessException(SAP_PORTAL_API_TOKEN_URL, e);
            } catch (RestClientResponseException e) {
                logger.error("Error retrieving SAP CCv2 token from fallback endpoint. Status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw new RuntimeException("Failed to retrieve SAP CCv2 access token from fallback endpoint. Status: " + e.getStatusCode(), e);
            }
            return cacheAndReturnToken(tokenResponse);
        }

        throw new IllegalStateException("Unexpected OAuth token retrieval state.");
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

        ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);
        return readJsonBody(response.getBody(), OAuthTokenResponse.class, "retrieving access token");
    }

    private String cacheAndReturnToken(OAuthTokenResponse tokenResponse) {
        if (tokenResponse.accessToken == null || tokenResponse.accessToken.isBlank()) {
            throw new RuntimeException("SAP CCv2 token endpoint returned an empty access token.");
        }

        long expiresIn = tokenResponse.expiresIn != null ? tokenResponse.expiresIn : 300L;
        long effectiveLifetime = Math.max(1L, expiresIn - 60L);

        cachedAccessToken = tokenResponse.accessToken;
        cachedTokenExpiresAt = Instant.now().plusSeconds(effectiveLifetime);
        logger.debug("Successfully generated SAP CCv2 access token");
        return cachedAccessToken;
    }

    private boolean isApiYaasDnsFailure(ResourceAccessException e, String endpoint) {
        if (endpoint == null) {
            return false;
        }

        URI uri = URI.create(endpoint);
        if (!"api.yaas.io".equalsIgnoreCase(uri.getHost())) {
            return false;
        }

        Throwable cause = e.getCause();
        return cause instanceof UnknownHostException;
    }

    private RuntimeException buildTokenEndpointAccessException(String endpoint, ResourceAccessException e) {
        logger.error("Unable to access SAP CCv2 token endpoint {}: {}", endpoint, e.getMessage());
        return new RuntimeException(
            "Cannot reach SAP CCv2 token endpoint: " + endpoint
                + ". If this endpoint uses api.yaas.io, switch to https://portalapi.commerce.ondemand.com/hybris/oauth2/v1/token.",
            e
        );
    }

    private void validateOauthConfiguration() {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("SAP CCv2 clientId is not configured. Please set sap.ccv2.api.clientId.");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("SAP CCv2 clientSecret is not configured. Please set sap.ccv2.api.clientSecret.");
        }
        if (tokenEndpoint == null || tokenEndpoint.isBlank()) {
            throw new IllegalArgumentException("SAP CCv2 token endpoint is not configured. Please set sap.ccv2.api.tokenEndpoint.");
        }
        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("SAP CCv2 resource is not configured. Please set sap.ccv2.api.resource.");
        }
    }

    private static class OAuthTokenResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("access_token")
        private String accessToken;

        @com.fasterxml.jackson.annotation.JsonProperty("expires_in")
        private Long expiresIn;
    }
}
