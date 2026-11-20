package com.yea.sap.ccv2.devops.mcp.server.tools;

import com.yea.sap.ccv2.devops.mcp.server.model.Build;
import com.yea.sap.ccv2.devops.mcp.server.model.BuildProgress;
import com.yea.sap.ccv2.devops.mcp.server.model.BuildResponse;
import com.yea.sap.ccv2.devops.mcp.server.model.BuildsResponse;
import com.yea.sap.ccv2.devops.mcp.server.model.CreateBuildRequest;
import com.yea.sap.ccv2.devops.mcp.server.model.CreateDeploymentRequest;
import com.yea.sap.ccv2.devops.mcp.server.model.DeployedBuild;
import com.yea.sap.ccv2.devops.mcp.server.model.DeploymentResponse;
import com.yea.sap.ccv2.devops.mcp.server.model.DeploymentsResponse;
import com.yea.sap.ccv2.devops.mcp.server.service.SapCcv2DevopsService;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

@Component
public class SapCCv2DevopsMcpTool
{

    private final SapCcv2DevopsService sapCcv2DevopsService;

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
        CreateBuildRequest request = new CreateBuildRequest(branch, name);
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
        name = "Get SAP Commerce Cloud Build Details",
        description = "Gets one build by its code, including its source branch and the properties returned by SAP."
    )
    public Build getBuild(
        @McpArg(description = "The exact SAP build code, for example the buildCode returned by a deployment.")
        final String buildCode
    ) {
        return sapCcv2DevopsService.getBuild(buildCode);
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
        name = "Get SAP Commerce Cloud Deployed Build",
        description = "Gets the latest deployed build in one environment, with separate fields for its buildCode, " +
            "source branch from build details, deployedTimestamp from the deployment, and available build properties. " +
            "A missing buildCode means no deployed build was found; a missing branch must not be inferred."
    )
    public DeployedBuild getDeployedBuild(
        @McpArg(description = "SAP environment code, for example d1, s1 or p1.")
        final String environmentCode
    ) {
        return sapCcv2DevopsService.getDeployedBuild(environmentCode);
    }

    @McpTool(
        name = "Get SAP Commerce Cloud Deployments",
        description = "Lists deployments for a subscription, optionally filtered by environment code and status. " +
            "For the latest deployed build in an environment, use its environmentCode, status DEPLOYED, and top 1. " +
            "Results with the DEPLOYED filter are ordered by deployedTimestamp descending."
    )
    public DeploymentsResponse getDeployments(
        @McpArg(description = "Number of deployments to retrieve. Defaults to 5.", required = false)
        final Integer top,
        @McpArg(description = "SAP environment code, for example d1, s1 or p1.", required = false)
        final String environmentCode,
        @McpArg(description = "SAP deployment status: SCHEDULED, DEPLOYING, DEPLOYED, UNDEPLOYED or FAIL.", required = false)
        final String status
    ) {
        return sapCcv2DevopsService.getDeployments(top, environmentCode, status);
    }
}

