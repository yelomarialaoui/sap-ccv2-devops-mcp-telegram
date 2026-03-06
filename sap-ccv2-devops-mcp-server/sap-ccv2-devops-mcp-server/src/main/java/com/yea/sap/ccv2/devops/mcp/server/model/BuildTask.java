package com.yea.sap.ccv2.devops.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BuildTask {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("startTimestamp")
    private String startTimestamp;
    
    @JsonProperty("task")
    private String task;
    
    // Constructors
    public BuildTask() {
    }
    
    public BuildTask(String name, String startTimestamp, String task) {
        this.name = name;
        this.startTimestamp = startTimestamp;
        this.task = task;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getStartTimestamp() {
        return startTimestamp;
    }
    
    public void setStartTimestamp(String startTimestamp) {
        this.startTimestamp = startTimestamp;
    }
    
    public String getTask() {
        return task;
    }
    
    public void setTask(String task) {
        this.task = task;
    }
}
