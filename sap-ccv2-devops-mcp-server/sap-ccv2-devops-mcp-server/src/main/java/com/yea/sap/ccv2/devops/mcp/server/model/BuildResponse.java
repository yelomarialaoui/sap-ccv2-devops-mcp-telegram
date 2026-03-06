package com.yea.sap.ccv2.devops.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BuildResponse {
    
    @JsonProperty("code")
    private String code;
    
    // Constructors
    public BuildResponse() {
    }
    
    public BuildResponse(String code) {
        this.code = code;
    }
    
    // Getters and Setters
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
}
