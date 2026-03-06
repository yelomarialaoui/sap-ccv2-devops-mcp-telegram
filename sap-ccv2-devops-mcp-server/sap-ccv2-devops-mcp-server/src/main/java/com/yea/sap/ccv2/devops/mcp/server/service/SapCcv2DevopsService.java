package com.yea.sap.ccv2.devops.mcp.server.service;

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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


@Service
public class SapCcv2DevopsService
{
    private static final Logger logger = LoggerFactory.getLogger(SapCcv2DevopsService.class);
    
    @Value("${sap.ccv2.api.subscriptionCode}")
    private String subscriptionCode;

    @Value("${sap.ccv2.api.token}")
    private String apiToken;

    private final RestTemplate restTemplate;
    private static final String SAP_PORTAL_API_BUILD_URL = "https://portalapi.commerce.ondemand.com/v2/subscriptions/{subscriptionCode}/builds";
    private static final String SAP_PORTAL_API_DEPLOY_URL = "https://portalapi.commerce.ondemand.com/v2/subscriptions/{subscriptionCode}/deployments";

    public SapCcv2DevopsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiToken);
        return headers;
    }

    public BuildResponse createBuild(CreateBuildRequest request) {
        String url = SAP_PORTAL_API_BUILD_URL.replace("{subscriptionCode}", subscriptionCode);
        
        HttpEntity<CreateBuildRequest> entity = new HttpEntity<>(request, getHeaders());
        
        return restTemplate.postForObject(url, entity, BuildResponse.class);
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

        if (apiToken == null || apiToken.isEmpty()) {
            logger.error("API token is null or empty");
            throw new IllegalArgumentException("SAP CCv2 API token is not configured. Please set SAP_CCV2_API_TOKEN");
        }
        
        // Build query string manually to avoid URL encoding of spaces
        StringBuilder queryString = new StringBuilder();
        queryString.append("?$top=").append(top);
        queryString.append("&$skip=").append(skip);
        queryString.append("&$statusNot=").append(statusNot);
        queryString.append("&$count=").append(count);
        queryString.append("&$orderby=").append(orderby);
        
        String urlWithParams = baseUrl + queryString.toString();
        logger.debug("Request URL: {}", urlWithParams);
        logger.debug("Token starts with: {}", apiToken.substring(0, Math.min(10, apiToken.length())) + "...");
        
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
        
        try {
            ResponseEntity<BuildsResponse> response = restTemplate.exchange(urlWithParams, HttpMethod.GET, entity, BuildsResponse.class);
            logger.info("Successfully retrieved builds. Response status: {}", response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("Authentication failed (401): {}", e.getMessage());
            throw new RuntimeException("Authentication failed. Please verify your SAP CCv2 credentials and token.", e);
        } catch (Exception e) {
            logger.error("Error retrieving builds: {}", e.getMessage(), e);
            throw e;
        }
    }

    public BuildProgress getBuildProgress(String buildCode) {
        String url = SAP_PORTAL_API_BUILD_URL.replace("{subscriptionCode}", subscriptionCode) + "/" + buildCode + "/progress";
        
        logger.info("getBuildProgress request: subscriptionCode={}, buildCode={}", subscriptionCode, buildCode);
        
        if (apiToken == null || apiToken.isEmpty()) {
            logger.error("API token is null or empty");
            throw new IllegalArgumentException("SAP CCv2 API token is not configured. Please set SAP_CCV2_API_TOKEN");
        }
        
        logger.debug("Request URL: {}", url);
        logger.debug("Token starts with: {}", apiToken.substring(0, Math.min(10, apiToken.length())) + "...");
        
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
        
        try {
            ResponseEntity<BuildProgress> response = restTemplate.exchange(url, HttpMethod.GET, entity, BuildProgress.class);
            logger.info("Successfully retrieved build progress. Response status: {}", response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("Authentication failed (401): {}", e.getMessage());
            throw new RuntimeException("Authentication failed. Please verify your SAP CCv2 credentials and token.", e);
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

        if (apiToken == null || apiToken.isEmpty()) {
            logger.error("API token is null or empty");
            throw new IllegalArgumentException("SAP CCv2 API token is not configured. Please set SAP_CCV2_API_TOKEN");
        }

        logger.debug("Request URL: {}", url);
        logger.debug("Token starts with: {}", apiToken.substring(0, Math.min(10, apiToken.length())) + "...");

        HttpEntity<CreateDeploymentRequest> entity = new HttpEntity<>(request, getHeaders());

        try {
            ResponseEntity<DeploymentResponse> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, DeploymentResponse.class);

            logger.info("Deployment created successfully. Response status: {}", response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.error("Authentication failed (401): {}", e.getMessage());
            throw new RuntimeException("Authentication failed. Please verify your SAP CCv2 credentials and token.", e);
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
            logger.debug("Token starts with: {}", apiToken.substring(0, Math.min(10, apiToken.length())) + "...");

            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());

            try
            {
                ResponseEntity<DeploymentsResponse> response =
                    restTemplate.exchange(urlWithParams, HttpMethod.GET, entity, DeploymentsResponse.class);
                logger.info("Successfully retrieved deployments. Response status: {}", response.getStatusCode());
                return response.getBody();
            }
            catch (HttpClientErrorException.Unauthorized e)
            {
                logger.error("Authentication failed (401): {}", e.getMessage());
                throw new RuntimeException("Authentication failed. Please verify your SAP CCv2 credentials and token.", e);
            }
            catch (Exception e)
            {
                logger.error("Error retrieving deployments: {}", e.getMessage(), e);
                throw e;
            }
        }
}
