package com.yea.sap.ccv2.devops.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class BuildProgress {
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @JsonProperty("numberOfTasks")
    private Integer numberOfTasks;
    
    @JsonProperty("percentage")
    private Integer percentage;
    
    @JsonProperty("startedTasks")
    private List<BuildTask> startedTasks;
    
    // Constructors
    public BuildProgress() {
    }
    
    public BuildProgress(String errorMessage, Integer numberOfTasks, Integer percentage, List<BuildTask> startedTasks) {
        this.errorMessage = errorMessage;
        this.numberOfTasks = numberOfTasks;
        this.percentage = percentage;
        this.startedTasks = startedTasks;
    }
    
    // Getters and Setters
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getNumberOfTasks() {
        return numberOfTasks;
    }
    
    public void setNumberOfTasks(Integer numberOfTasks) {
        this.numberOfTasks = numberOfTasks;
    }
    
    public Integer getPercentage() {
        return percentage;
    }
    
    public void setPercentage(Integer percentage) {
        this.percentage = percentage;
    }
    
    public List<BuildTask> getStartedTasks() {
        return startedTasks;
    }
    
    public void setStartedTasks(List<BuildTask> startedTasks) {
        this.startedTasks = startedTasks;
    }
}
