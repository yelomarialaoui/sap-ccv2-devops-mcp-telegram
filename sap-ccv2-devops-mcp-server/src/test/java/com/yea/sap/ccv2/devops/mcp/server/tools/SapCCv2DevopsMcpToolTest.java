package com.yea.sap.ccv2.devops.mcp.server.tools;

import com.yea.sap.ccv2.devops.mcp.server.model.CreateBuildRequest;
import com.yea.sap.ccv2.devops.mcp.server.model.CreateDeploymentRequest;
import com.yea.sap.ccv2.devops.mcp.server.service.SapCcv2DevopsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SapCCv2DevopsMcpToolTest {

    private final SapCcv2DevopsService service = mock(SapCcv2DevopsService.class);
    private final SapCCv2DevopsMcpTool tool = new SapCCv2DevopsMcpTool(service);

    @Test
    void createBuildSapCcv2_shouldSendBranchAndNameToService() {
        tool.createBuildSapCcv2("sqli-v8.6.0.2-ccv2", "test2");

        ArgumentCaptor<CreateBuildRequest> captor = ArgumentCaptor.forClass(CreateBuildRequest.class);
        verify(service).createBuild(captor.capture());
        assertEquals("sqli-v8.6.0.2-ccv2", captor.getValue().getBranch());
        assertEquals("test2", captor.getValue().getName());
    }

    @Test
    void createDeployment_shouldMapAllArgumentsIntoRequest() {
        tool.createDeployment("build-1", "dev", "NONE", "ROLLING_UPDATE");

        ArgumentCaptor<CreateDeploymentRequest> captor = ArgumentCaptor.forClass(CreateDeploymentRequest.class);
        verify(service).createDeployment(captor.capture());
        assertEquals("build-1", captor.getValue().getBuildCode());
        assertEquals("dev", captor.getValue().getEnvironmentCode());
        assertEquals("NONE", captor.getValue().getDatabaseUpdateMode());
        assertEquals("ROLLING_UPDATE", captor.getValue().getStrategy());
    }

    @Test
    void getBuilds_shouldDelegateTopToService() {
        tool.getBuilds(3);

        verify(service).getBuilds(3);
    }

    @Test
    void getBuildProgress_shouldDelegateBuildCodeToService() {
        tool.getBuildProgress("build-1");

        verify(service).getBuildProgress("build-1");
    }

    @Test
    void getDeployments_shouldDelegateTopToService() {
        tool.getDeployments(null);

        verify(service).getDeployments(null);
    }
}
