package com.yea.sap.ccv2.devops.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Build {
    
    @JsonProperty("applicationCode")
    private String applicationCode;
    
    @JsonProperty("applicationDefinitionVersion")
    private String applicationDefinitionVersion;
    
    @JsonProperty("branch")
    private String branch;
    
    @JsonProperty("buildEndTimestamp")
    private String buildEndTimestamp;
    
    @JsonProperty("buildStartTimestamp")
    private String buildStartTimestamp;
    
    @JsonProperty("buildVersion")
    private String buildVersion;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("createdBy")
    private String createdBy;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("properties")
    private List<BuildProperty> properties;
    
    @JsonProperty("status")
    private String status;
    
    // Constructors
    public Build() {
    }
    
    // Getters and Setters
    public String getApplicationCode() {
        return applicationCode;
    }
    
    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }
    
    public String getApplicationDefinitionVersion() {
        return applicationDefinitionVersion;
    }
    
    public void setApplicationDefinitionVersion(String applicationDefinitionVersion) {
        this.applicationDefinitionVersion = applicationDefinitionVersion;
    }
    
    public String getBranch() {
        return branch;
    }
    
    public void setBranch(String branch) {
        this.branch = branch;
    }
    
    public String getBuildEndTimestamp() {
        return buildEndTimestamp;
    }
    
    public void setBuildEndTimestamp(String buildEndTimestamp) {
        this.buildEndTimestamp = buildEndTimestamp;
    }
    
    public String getBuildStartTimestamp() {
        return buildStartTimestamp;
    }
    
    public void setBuildStartTimestamp(String buildStartTimestamp) {
        this.buildStartTimestamp = buildStartTimestamp;
    }
    
    public String getBuildVersion() {
        return buildVersion;
    }
    
    public void setBuildVersion(String buildVersion) {
        this.buildVersion = buildVersion;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<BuildProperty> getProperties() {
        return properties;
    }
    
    public void setProperties(List<BuildProperty> properties) {
        this.properties = properties;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
