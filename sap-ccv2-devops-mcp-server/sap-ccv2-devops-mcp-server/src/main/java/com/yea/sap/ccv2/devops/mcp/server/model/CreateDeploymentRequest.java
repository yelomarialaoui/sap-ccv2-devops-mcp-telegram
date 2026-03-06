package com.yea.sap.ccv2.devops.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateDeploymentRequest {
    
    @JsonProperty("buildCode")
    private String buildCode;
    
    @JsonProperty("databaseUpdateMode")
    private String databaseUpdateMode;
    
    @JsonProperty("environmentCode")
    private String environmentCode;
    
    @JsonProperty("strategy")
    private String strategy;
    
    // Constructors
    public CreateDeploymentRequest() {
    }
    
    public CreateDeploymentRequest(String buildCode, String databaseUpdateMode, String environmentCode, String strategy) {
        this.buildCode = buildCode;
        this.databaseUpdateMode = databaseUpdateMode;
        this.environmentCode = environmentCode;
        this.strategy = strategy;
    }
    
    // Getters and Setters
    public String getBuildCode() {
        return buildCode;
    }
    
    public void setBuildCode(String buildCode) {
        this.buildCode = buildCode;
    }
    
    public String getDatabaseUpdateMode() {
        return databaseUpdateMode;
    }
    
    public void setDatabaseUpdateMode(String databaseUpdateMode) {
        this.databaseUpdateMode = databaseUpdateMode;
    }
    
    public String getEnvironmentCode() {
        return environmentCode;
    }
    
    public void setEnvironmentCode(String environmentCode) {
        this.environmentCode = environmentCode;
    }
    
    public String getStrategy() {
        return strategy;
    }
    
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}
