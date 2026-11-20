package com.yea.sap.ccv2.devops.mcp.server.model;

import java.time.OffsetDateTime;
import java.util.List;

public record DeployedBuild(String environmentCode, String buildCode, String branch,
                            OffsetDateTime deployedTimestamp, List<BuildProperty> buildProperties) {
}
