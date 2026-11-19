package com.yea.sap.ccv2.devops.mcp.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yea.sap.ccv2.devops.mcp.server.client.SapCcv2ApiClient;
import com.yea.sap.ccv2.devops.mcp.server.client.SapCcv2TokenProvider;
import com.yea.sap.ccv2.devops.mcp.server.model.BuildsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        ObjectMapper objectMapper = new ObjectMapper();
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
