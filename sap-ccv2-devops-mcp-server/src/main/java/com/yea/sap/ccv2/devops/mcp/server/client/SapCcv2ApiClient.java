package com.yea.sap.ccv2.devops.mcp.server.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Component
public class SapCcv2ApiClient {

    private static final Logger logger = LoggerFactory.getLogger(SapCcv2ApiClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SapCcv2TokenProvider tokenProvider;

    public SapCcv2ApiClient(RestTemplate restTemplate, ObjectMapper objectMapper, SapCcv2TokenProvider tokenProvider) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.tokenProvider = tokenProvider;
    }

    public String send(HttpMethod method, String url, Object body, String operation) {
        logger.debug("Request URL: {}", url);
        HttpEntity<Object> entity = new HttpEntity<>(body, getHeaders());
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, method, entity, byte[].class);
            logger.info("SAP CCv2 call succeeded while {}. Response status: {}", operation, response.getStatusCode());
            return decodeResponseBody(response.getBody());
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("Authentication failed while {} (401): {}", operation, e.getResponseBodyAsString());
            throw new RuntimeException("Authentication failed. Please verify your SAP CCv2 credentials and token.", e);
        } catch (RestClientResponseException e) {
            logger.error("HTTP error while {}. Status: {}, body: {}", operation, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("SAP CCv2 request failed while " + operation + ". Status: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            logger.error("Unable to reach SAP CCv2 while {}: {}", operation, e.getMessage());
            throw new RuntimeException("SAP CCv2 request failed while " + operation + ": " + e.getMessage(), e);
        }
    }

    public <T> T readJson(String responseBody, Class<T> responseType, String operation) {
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

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("x-approuter-authorization", "Bearer " + tokenProvider.getAccessToken());
        return headers;
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
}
