package com.yea.sap.ccv2.devops.mcp.server.model;

import java.util.List;

public class DeploymentsResponse {

    private List<DeploymentInfo> value;
    private Integer count;

    // getters and setters
    public List<DeploymentInfo> getValue() {
        return value;
    }
    public void setValue(List<DeploymentInfo> value) {
        this.value = value;
    }
    public Integer getCount() {
        return count;
    }
    public void setCount(Integer count) {
        this.count = count;
    }
}