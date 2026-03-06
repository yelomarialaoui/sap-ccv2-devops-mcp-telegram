package com.yea.sap.ccv2.devops.mcp.server.model;

import java.time.OffsetDateTime;

public class DeploymentInfo {
    private String code;
    private String subscriptionCode;
    private String createdBy;
    private OffsetDateTime createdTimestamp;
    private String buildCode;
    private String environmentCode;
    private String databaseUpdateMode;
    private String strategy;
    private OffsetDateTime scheduledTimestamp;
    private OffsetDateTime deployedTimestamp;
    private OffsetDateTime failedTimestamp;
    private OffsetDateTime undeployedTimestamp;
    private String status;
    private Cancelation cancelation;

    public String getCode()
    {
        return code;
    }

    public void setCode(final String code)
    {
        this.code = code;
    }

    public String getSubscriptionCode()
    {
        return subscriptionCode;
    }

    public void setSubscriptionCode(final String subscriptionCode)
    {
        this.subscriptionCode = subscriptionCode;
    }

    public String getCreatedBy()
    {
        return createdBy;
    }

    public void setCreatedBy(final String createdBy)
    {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedTimestamp()
    {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(final OffsetDateTime createdTimestamp)
    {
        this.createdTimestamp = createdTimestamp;
    }

    public String getBuildCode()
    {
        return buildCode;
    }

    public void setBuildCode(final String buildCode)
    {
        this.buildCode = buildCode;
    }

    public String getEnvironmentCode()
    {
        return environmentCode;
    }

    public void setEnvironmentCode(final String environmentCode)
    {
        this.environmentCode = environmentCode;
    }

    public String getDatabaseUpdateMode()
    {
        return databaseUpdateMode;
    }

    public void setDatabaseUpdateMode(final String databaseUpdateMode)
    {
        this.databaseUpdateMode = databaseUpdateMode;
    }

    public String getStrategy()
    {
        return strategy;
    }

    public void setStrategy(final String strategy)
    {
        this.strategy = strategy;
    }

    public OffsetDateTime getScheduledTimestamp()
    {
        return scheduledTimestamp;
    }

    public void setScheduledTimestamp(final OffsetDateTime scheduledTimestamp)
    {
        this.scheduledTimestamp = scheduledTimestamp;
    }

    public OffsetDateTime getDeployedTimestamp()
    {
        return deployedTimestamp;
    }

    public void setDeployedTimestamp(final OffsetDateTime deployedTimestamp)
    {
        this.deployedTimestamp = deployedTimestamp;
    }

    public OffsetDateTime getFailedTimestamp()
    {
        return failedTimestamp;
    }

    public void setFailedTimestamp(final OffsetDateTime failedTimestamp)
    {
        this.failedTimestamp = failedTimestamp;
    }

    public OffsetDateTime getUndeployedTimestamp()
    {
        return undeployedTimestamp;
    }

    public void setUndeployedTimestamp(final OffsetDateTime undeployedTimestamp)
    {
        this.undeployedTimestamp = undeployedTimestamp;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(final String status)
    {
        this.status = status;
    }

    public Cancelation getCancelation()
    {
        return cancelation;
    }

    public void setCancelation(final Cancelation cancelation)
    {
        this.cancelation = cancelation;
    }

    public static class Cancelation {
        private String canceledBy;
        private OffsetDateTime startTimestamp;
        private OffsetDateTime finishedTimestamp;
        private Boolean failed;
        private Boolean rollbackDatabase;

        public String getCanceledBy()
        {
            return canceledBy;
        }

        public void setCanceledBy(final String canceledBy)
        {
            this.canceledBy = canceledBy;
        }

        public OffsetDateTime getStartTimestamp()
        {
            return startTimestamp;
        }

        public void setStartTimestamp(final OffsetDateTime startTimestamp)
        {
            this.startTimestamp = startTimestamp;
        }

        public OffsetDateTime getFinishedTimestamp()
        {
            return finishedTimestamp;
        }

        public void setFinishedTimestamp(final OffsetDateTime finishedTimestamp)
        {
            this.finishedTimestamp = finishedTimestamp;
        }

        public Boolean getFailed()
        {
            return failed;
        }

        public void setFailed(final Boolean failed)
        {
            this.failed = failed;
        }

        public Boolean getRollbackDatabase()
        {
            return rollbackDatabase;
        }

        public void setRollbackDatabase(final Boolean rollbackDatabase)
        {
            this.rollbackDatabase = rollbackDatabase;
        }
    }
}