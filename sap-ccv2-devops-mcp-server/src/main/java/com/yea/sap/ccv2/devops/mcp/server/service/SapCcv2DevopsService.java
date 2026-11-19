package com.yea.sap.ccv2.devops.mcp.server.service;

import com.yea.sap.ccv2.devops.mcp.server.client.SapCcv2ApiClient;
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
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
public class SapCcv2DevopsService
{
    private static final Logger logger = LoggerFactory.getLogger(SapCcv2DevopsService.class);

    private static final String SAP_PORTAL_API_BUILD_URL = "https://portalapi.commerce.ondemand.com/v2/subscriptions/{subscriptionCode}/builds";
    private static final String SAP_PORTAL_API_DEPLOY_URL = "https://portalapi.commerce.ondemand.com/v2/subscriptions/{subscriptionCode}/deployments";
    private static final int DEFAULT_TOP = 5;

    private final SapCcv2ApiClient apiClient;
    private final String subscriptionCode;

    public SapCcv2DevopsService(SapCcv2ApiClient apiClient,
                                @Value("${sap.ccv2.api.subscriptionCode}") String subscriptionCode) {
        this.apiClient = apiClient;
        this.subscriptionCode = subscriptionCode;
    }

    private String subscriptionUrl(String template) {
        return template.replace("{subscriptionCode}", subscriptionCode);
    }

    public BuildResponse createBuild(CreateBuildRequest request) {
        logger.info("createBuild request: subscriptionCode={}, branch={}, name={}",
                    subscriptionCode, request.getBranch(), request.getName());

        String rawBody = apiClient.send(HttpMethod.POST, subscriptionUrl(SAP_PORTAL_API_BUILD_URL), request, "creating build");
        try {
            return apiClient.readJson(rawBody, BuildResponse.class, "creating build");
        } catch (RuntimeException e) {
            logger.warn("createBuild: response is not the expected JSON, build was accepted (HTTP 2xx). Body: {}", rawBody);
            return new BuildResponse();
        }
    }

    public BuildsResponse getBuilds(Integer top) {
        int effectiveTop = top != null ? top : DEFAULT_TOP;
        logger.info("getBuilds request: subscriptionCode={}, top={}", subscriptionCode, effectiveTop);

        String url = subscriptionUrl(SAP_PORTAL_API_BUILD_URL)
            + "?$top=" + effectiveTop + "&$skip=0&statusNot=DELETED&$count=true&$orderby=buildStartTimestamp desc";
        return apiClient.readJson(apiClient.send(HttpMethod.GET, url, null, "retrieving builds"), BuildsResponse.class, "retrieving builds");
    }

    public BuildProgress getBuildProgress(String buildCode) {
        logger.info("getBuildProgress request: subscriptionCode={}, buildCode={}", subscriptionCode, buildCode);

        String url = subscriptionUrl(SAP_PORTAL_API_BUILD_URL) + "/" + buildCode + "/progress";
        return apiClient.readJson(apiClient.send(HttpMethod.GET, url, null, "retrieving build progress"), BuildProgress.class, "retrieving build progress");
    }

    public DeploymentResponse createDeployment(CreateDeploymentRequest request) {
        logger.info("createDeployment request: subscriptionCode={}, buildCode={}, environmentCode={}, databaseUpdateMode={}, strategy={}",
            subscriptionCode,
            request.getBuildCode(),
            request.getEnvironmentCode(),
            request.getDatabaseUpdateMode(),
            request.getStrategy());

        String url = subscriptionUrl(SAP_PORTAL_API_DEPLOY_URL);
        return apiClient.readJson(apiClient.send(HttpMethod.POST, url, request, "creating deployment"), DeploymentResponse.class, "creating deployment");
    }

    public DeploymentsResponse getDeployments(Integer top) {
        int effectiveTop = top != null ? top : DEFAULT_TOP;
        logger.info("getDeployments request: subscriptionCode={}, top={}", subscriptionCode, effectiveTop);

        String url = subscriptionUrl(SAP_PORTAL_API_DEPLOY_URL)
            + "?$top=" + effectiveTop + "&$skip=0&$count=true&$orderby=createdTimestamp desc";
        return apiClient.readJson(apiClient.send(HttpMethod.GET, url, null, "retrieving deployments"), DeploymentsResponse.class, "retrieving deployments");
    }
}
