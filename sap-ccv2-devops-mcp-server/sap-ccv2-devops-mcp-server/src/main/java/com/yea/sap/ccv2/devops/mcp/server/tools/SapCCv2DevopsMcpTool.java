package com.yea.sap.ccv2.devops.mcp.server.tools;

import com.yea.sap.ccv2.devops.mcp.server.model.BuildProgress;
import com.yea.sap.ccv2.devops.mcp.server.model.BuildResponse;
import com.yea.sap.ccv2.devops.mcp.server.model.BuildsResponse;
import com.yea.sap.ccv2.devops.mcp.server.model.CreateBuildRequest;
import com.yea.sap.ccv2.devops.mcp.server.model.CreateDeploymentRequest;
import com.yea.sap.ccv2.devops.mcp.server.model.DeploymentResponse;
import com.yea.sap.ccv2.devops.mcp.server.model.DeploymentsResponse;
import com.yea.sap.ccv2.devops.mcp.server.service.SapCcv2DevopsService;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SapCCv2DevopsMcpTool
{

    @Value("${sap.ccv2.api.applicationCode}")
    private String applicationCode;

    SapCcv2DevopsService sapCcv2DevopsService;

    public SapCCv2DevopsMcpTool(SapCcv2DevopsService sapCcv2DevopsService) {
        this.sapCcv2DevopsService = sapCcv2DevopsService;
    }

    @McpTool(
        name = "Create SAP Commerce Cloud Build",
        description = "This API operation and its parameters support the request to create a SAP Commerce Cloud build. " +
                     "POST https://portalapi.commerce.ondemand.com/v2/subscriptions/{subscriptionCode}/builds"
    )
    public BuildResponse createBuildSapCcv2(
        
        @McpArg(description = "The branch name for the build (required)")
        final String branch,
        
        @McpArg(description = "The name of the build (required)")
        final String name
    ) {
        CreateBuildRequest request = new CreateBuildRequest(applicationCode, branch, name);
        return sapCcv2DevopsService.createBuild(request);
    }

    @McpTool(
        name = "Get SAP Commerce Cloud Builds",
        description = "This API operation and its parameters support the request to get all SAP Commerce Cloud builds associated with a specific subscription. " +
                     "GET https://portalapi.commerce.ondemand.com/v2/subscriptions/{subscriptionCode}/builds"
    )
    public BuildsResponse getBuilds(
        @McpArg(description = "The number of items to return. This parameter determines the page size. (optional)", required = false)
        final Integer top
    ) {
        return sapCcv2DevopsService.getBuilds(top);
    }

    @McpTool(
        name = "Get SAP Commerce Cloud Build Progress",
        description = "This API operation and its parameters support the request to get SAP Commerce Cloud build progress. " +
                     "GET https://portalapi.commerce.ondemand.com/v2/subscriptions/{subscriptionCode}/builds/{buildCode}/progress"
    )
    public BuildProgress getBuildProgress(
        @McpArg(description = "The code of a specific build. You can find the build code in the Cloud Portal URL. It appears after builds/ in the URL.")
        final String buildCode
    ) {
        return sapCcv2DevopsService.getBuildProgress(buildCode);
    }


    @McpTool(
        name = "Create SAP Commerce Cloud Deployment",
        description = "This API operation and its parameters support the request to create a SAP Commerce Cloud deployment. " +
            "POST https://portalapi.commerce.ondemand.com/v2/subscriptions/{subscriptionCode}/deployments"
    )
    public DeploymentResponse createDeployment(
        @McpArg(description = "The code of the build you want to deploy. You can find the build code in the Cloud Portal URL after builds/.")
        final String buildCode,

        @McpArg(description = "The environment where the build will be deployed (for example: dev, staging, or prod).")
        final String environmentCode,

        @McpArg(description = "Defines how the database should be handled during deployment. Possible values: NONE, UPDATE, INITIALIZE.")
        final String databaseUpdateMode,

        @McpArg(description = "Deployment strategy. Possible values: ROLLING_UPDATE, RECREATE, GREEN.")
        final String strategy
    ) {

        CreateDeploymentRequest request = new CreateDeploymentRequest();
        request.setBuildCode(buildCode);
        request.setEnvironmentCode(environmentCode);
        request.setDatabaseUpdateMode(databaseUpdateMode);
        request.setStrategy(strategy);

        return sapCcv2DevopsService.createDeployment(request);
    }

    @McpTool(
        name = "Get SAP Commerce Cloud Deployments",
        description = "This API operation retrieves all deployments associated with the subscription. " +
            "You can filter by environmentCode, buildCode, status, and limit the number of results."
    )
    public DeploymentsResponse getDeployments(
        @McpArg(description = "Optional. Number of latest deployments to retrieve. Defaults to 5.")
        final Integer top
    ) {
        return sapCcv2DevopsService.getDeployments(top);
    }
}

