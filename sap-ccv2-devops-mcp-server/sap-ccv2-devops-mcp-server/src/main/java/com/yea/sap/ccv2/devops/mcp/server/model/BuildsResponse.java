package com.yea.sap.ccv2.devops.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class BuildsResponse {
    
    @JsonProperty("count")
    private Integer count;
    
    @JsonProperty("value")
    private List<Build> value;
    
    // Constructors
    public BuildsResponse() {
    }
    
    public BuildsResponse(Integer count, List<Build> value) {
        this.count = count;
        this.value = value;
    }
    
    // Getters and Setters
    public Integer getCount() {
        return count;
    }
    
    public void setCount(Integer count) {
        this.count = count;
    }
    
    public List<Build> getValue() {
        return value;
    }
    
    public void setValue(List<Build> value) {
        this.value = value;
    }
}
