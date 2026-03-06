package com.yea.sap.ccv2.devops.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BuildProperty {
    
    @JsonProperty("key")
    private String key;
    
    @JsonProperty("value")
    private Object value;
    
    // Constructors
    public BuildProperty() {
    }
    
    public BuildProperty(String key, Object value) {
        this.key = key;
        this.value = value;
    }
    
    // Getters and Setters
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
}
