package com.yea.sap.ccv2.devops.mcp.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yea.sap.ccv2.devops.mcp.server.client.SapCcv2ApiClient;
import com.yea.sap.ccv2.devops.mcp.server.client.SapCcv2TokenProvider;
import com.yea.sap.ccv2.devops.mcp.server.model.Build;
import com.yea.sap.ccv2.devops.mcp.server.model.BuildProgress;
import com.yea.sap.ccv2.devops.mcp.server.model.BuildsResponse;
import com.yea.sap.ccv2.devops.mcp.server.model.CreateBuildRequest;
import com.yea.sap.ccv2.devops.mcp.server.model.CreateDeploymentRequest;
import com.yea.sap.ccv2.devops.mcp.server.model.DeploymentsResponse;
import com.yea.sap.ccv2.devops.mcp.server.tools.SapCCv2DevopsMcpTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.UnknownHostException;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SapCcv2DevopsServiceTest {

    private SapCcv2DevopsService service;
    private MockRestServiceServer server;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        service = buildService("https://auth.example.com/oauth2/token");
    }

    private SapCcv2DevopsService buildService(String tokenEndpoint) {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        SapCcv2TokenProvider tokenProvider = new SapCcv2TokenProvider(
            restTemplate, objectMapper, "client-123", "secret-123", tokenEndpoint, "https://portalapi.commerce.ondemand.com");
        SapCcv2ApiClient apiClient = new SapCcv2ApiClient(restTemplate, objectMapper, tokenProvider);
        return new SapCcv2DevopsService(apiClient, "sub-123");
    }

    @Test
    void getBuilds_shouldParseJsonBodyEvenWhenContentTypeIsTextHtml() {
        expectTokenRequest();

        server.expect(requestTo("https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/builds?$top=5&$skip=0&statusNot=DELETED&$count=true&$orderby=buildStartTimestamp%20desc"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("x-approuter-authorization", "Bearer access-token-abc"))
            .andRespond(withSuccess("{\"count\":1,\"value\":[]}", MediaType.TEXT_HTML));

        BuildsResponse response = service.getBuilds(5);

        assertEquals(1, response.getCount());
        assertEquals(0, response.getValue().size());
    }

    @Test
    void getBuilds_shouldFailWithClearMessageWhenBodyIsNotJson() {
        expectTokenRequest();

        server.expect(requestTo("https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/builds?$top=5&$skip=0&statusNot=DELETED&$count=true&$orderby=buildStartTimestamp%20desc"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("<html>error</html>", MediaType.TEXT_HTML));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.getBuilds(5));

        assertEquals("SAP CCv2 returned a non-JSON response while retrieving builds.", exception.getMessage());
    }

    @Test
    void getBuilds_shouldFallbackToPortalTokenEndpointWhenApiYaasHostCannotBeResolved() {
        service = buildService("https://api.yaas.io/hybris/oauth2/v1/token");

        server.expect(requestTo("https://api.yaas.io/hybris/oauth2/v1/token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withException(new UnknownHostException("api.yaas.io")));

        server.expect(requestTo("https://portalapi.commerce.ondemand.com/hybris/oauth2/v1/token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{\"access_token\":\"access-token-abc\",\"expires_in\":300}", MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/builds?$top=5&$skip=0&statusNot=DELETED&$count=true&$orderby=buildStartTimestamp%20desc"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("x-approuter-authorization", "Bearer access-token-abc"))
            .andRespond(withSuccess("{\"count\":1,\"value\":[]}", MediaType.APPLICATION_JSON));

        BuildsResponse response = service.getBuilds(5);

        assertEquals(1, response.getCount());
    }

    @Test
    void getBuildProgress_shouldCallProgressEndpointOfBuild() {
        expectTokenRequest();

        server.expect(requestTo("https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/builds/build-1/progress"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"percentage\":40,\"numberOfTasks\":10}", MediaType.APPLICATION_JSON));

        BuildProgress progress = service.getBuildProgress("build-1");

        assertEquals(40, progress.getPercentage());
        assertEquals(10, progress.getNumberOfTasks());
    }

    @Test
    void createDeployment_shouldPostRequestAndReturnDeploymentCode() {
        expectTokenRequest();

        CreateDeploymentRequest request = new CreateDeploymentRequest();
        request.setBuildCode("build-1");
        request.setEnvironmentCode("dev");
        request.setDatabaseUpdateMode("NONE");
        request.setStrategy("ROLLING_UPDATE");

        server.expect(requestTo("https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/deployments"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"buildCode\":\"build-1\"")))
            .andRespond(withSuccess("{\"code\":\"deploy-9\"}", MediaType.APPLICATION_JSON));

        assertEquals("deploy-9", service.createDeployment(request).getCode());
    }

    @Test
    void createDeployment_shouldFailWhenResponseBodyIsEmpty() {
        expectTokenRequest();

        server.expect(requestTo("https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/deployments"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThrows(RuntimeException.class, () -> service.createDeployment(new CreateDeploymentRequest()));
    }

    @Test
    void getDeployments_shouldUseTopFiveWhenNotProvided() {
        expectTokenRequest();

        server.expect(requestTo("https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/deployments?$top=5&$skip=0&$count=true&$orderby=createdTimestamp%20desc"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("{\"count\":0,\"value\":[]}", MediaType.APPLICATION_JSON));

        DeploymentsResponse response = service.getDeployments(null, null, null);

        assertEquals(0, response.getCount());
    }

    @Test
    void getDeployments_returnsLatestDeployedBuildForRequestedEnvironment() {
        expectTokenRequest();
        server.expect(requestTo("https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/deployments"
                + "?$top=1&$skip=0&$count=true&$orderby=deployedTimestamp%20desc&environmentCode=s1&status=DEPLOYED"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {"count":3,"value":[{"code":"deployment-1","environmentCode":"s1",
                "status":"DEPLOYED","buildCode":"build-42"}]}
                """, MediaType.APPLICATION_JSON));

        DeploymentsResponse response = new SapCCv2DevopsMcpTool(service).getDeployments(1, "s1", "DEPLOYED");

        assertEquals(1, response.getValue().size());
        assertEquals("build-42", response.getValue().getFirst().getBuildCode());
        server.verify();
    }

    @Test
    void getBuild_returnsBranchAndAvailablePropertiesForTheDeployedBuildCode() {
        expectTokenRequest();
        server.expect(requestTo("https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/builds/build-42"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {"code":"build-42","branch":"release/2026.11","hasSnapshot":true,"deployed":true,
                "status":"SUCCESS","properties":[{"key":"example.commit","value":"a5be421"}]}
                """, MediaType.APPLICATION_JSON));

        Build build = new SapCCv2DevopsMcpTool(service).getBuild("build-42");

        assertEquals("release/2026.11", build.getBranch());
        assertEquals("a5be421", build.getProperties().getFirst().getValue());
        server.verify();
    }

    @Test
    void getDeployedBuild_returnsTheBranchFromBuildDetailsAndDateFromDeployment() {
        expectTokenRequest();
        server.expect(requestTo("https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/deployments"
                + "?$top=1&$skip=0&$count=true&$orderby=deployedTimestamp%20desc&environmentCode=p1&status=DEPLOYED"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {"count":1,"value":[{"buildCode":"20261105.1","environmentCode":"p1",
                "status":"DEPLOYED","deployedTimestamp":"2026-11-05T17:31:33Z"}]}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/builds/20261105.1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {"code":"20261105.1","branch":"release/2026.11","buildEndTimestamp":"2026-11-05T16:00:00Z"}
                """, MediaType.APPLICATION_JSON));

        var deployedBuild = new SapCCv2DevopsMcpTool(service).getDeployedBuild("p1");

        assertEquals("20261105.1", deployedBuild.buildCode());
        assertEquals("release/2026.11", deployedBuild.branch());
        assertEquals(OffsetDateTime.parse("2026-11-05T17:31:33Z"), deployedBuild.deployedTimestamp());
        server.verify();
    }

    @Test
    void getDeployedBuild_leavesBranchUnknownWhenSapOmitsIt() {
        expectTokenRequest();
        server.expect(requestTo("https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/deployments"
                + "?$top=1&$skip=0&$count=true&$orderby=deployedTimestamp%20desc&environmentCode=p1&status=DEPLOYED"))
            .andRespond(withSuccess("""
                {"value":[{"buildCode":"20261105.1","deployedTimestamp":"2026-11-05T17:31:33Z"}]}
                """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/builds/20261105.1"))
            .andRespond(withSuccess("""
                {"code":"20261105.1"}
                """, MediaType.APPLICATION_JSON));

        var deployedBuild = new SapCCv2DevopsMcpTool(service).getDeployedBuild("p1");

        assertEquals("20261105.1", deployedBuild.buildCode());
        assertNull(deployedBuild.branch());
        assertEquals(OffsetDateTime.parse("2026-11-05T17:31:33Z"), deployedBuild.deployedTimestamp());
        server.verify();
    }

    @Test
    void createBuild_shouldReturnEmptyResponseWhenAcceptedBodyIsNotJson() {
        expectTokenRequest();

        server.expect(requestTo("https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/builds"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("not-json-gzip-garbage", MediaType.APPLICATION_JSON));

        CreateBuildRequest request = new CreateBuildRequest("main", "build-name");

        assertNull(service.createBuild(request).getCode());
    }

    private void expectTokenRequest() {
        server.expect(requestTo("https://auth.example.com/oauth2/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("client_id=client-123")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("client_secret=secret-123")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=client_credentials")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("resource=https%3A%2F%2Fportalapi.commerce.ondemand.com")))
            .andRespond(withSuccess("{\"access_token\":\"access-token-abc\",\"expires_in\":300}", MediaType.APPLICATION_JSON));
    }
}
