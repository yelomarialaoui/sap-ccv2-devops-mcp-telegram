package com.yea.sap.ccv2.devops.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateBuildRequest {
    
    @JsonProperty("applicationCode")
    private String applicationCode;
    
    @JsonProperty("branch")
    private String branch;
    
    @JsonProperty("name")
    private String name;
    
    // Constructors
    public CreateBuildRequest() {
    }
    
    public CreateBuildRequest(String branch, String name) {
        this.branch = branch;
        this.name = name;
    }
    
    public CreateBuildRequest(String applicationCode, String branch, String name) {
        this.applicationCode = applicationCode;
        this.branch = branch;
        this.name = name;
    }
    
    // Getters and Setters
    public String getApplicationCode() {
        return applicationCode;
    }
    
    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }
    
    public String getBranch() {
        return branch;
    }
    
    public void setBranch(String branch) {
        this.branch = branch;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
