package com.yea.sap.ccv2.devops.mcp.server.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yea.sap.ccv2.devops.mcp.server.model.BuildsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SapCcv2ApiClientTest {

    private static final String URL = "https://portalapi.commerce.ondemand.com/v2/subscriptions/sub-123/builds";

    @Test
    void send_shouldDecompressGzipResponseBody() throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        SapCcv2TokenProvider tokenProvider = tokenProvider(restTemplate, server);
        server.expect(requestTo(URL))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("x-approuter-authorization", "Bearer access-token-abc"))
            .andRespond(withSuccess(gzip("{\"count\":2,\"value\":[]}"), MediaType.APPLICATION_JSON));

        String body = new SapCcv2ApiClient(restTemplate, new ObjectMapper(), tokenProvider)
            .send(HttpMethod.GET, URL, null, "retrieving builds");

        assertEquals("{\"count\":2,\"value\":[]}", body);
    }

    @Test
    void send_shouldReportAuthenticationFailureOn401() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        SapCcv2TokenProvider tokenProvider = tokenProvider(restTemplate, server);
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        SapCcv2ApiClient client = new SapCcv2ApiClient(restTemplate, new ObjectMapper(), tokenProvider);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> client.send(HttpMethod.GET, URL, null, "retrieving builds"));

        assertEquals("Authentication failed. Please verify your SAP CCv2 credentials and token.", exception.getMessage());
    }

    @Test
    void send_shouldReportStatusOnOtherHttpErrors() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        SapCcv2TokenProvider tokenProvider = tokenProvider(restTemplate, server);
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        SapCcv2ApiClient client = new SapCcv2ApiClient(restTemplate, new ObjectMapper(), tokenProvider);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> client.send(HttpMethod.GET, URL, null, "retrieving builds"));

        assertEquals("SAP CCv2 request failed while retrieving builds. Status: 403 FORBIDDEN", exception.getMessage());
    }

    @Test
    void readJson_shouldFailOnBlankBody() {
        SapCcv2ApiClient client = new SapCcv2ApiClient(new RestTemplate(), new ObjectMapper(), null);

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> client.readJson(" ", BuildsResponse.class, "retrieving builds"));

        assertEquals("SAP CCv2 returned an empty response while retrieving builds.", exception.getMessage());
    }

    private SapCcv2TokenProvider tokenProvider(RestTemplate restTemplate, MockRestServiceServer server) {
        server.expect(requestTo("https://auth.example.com/oauth2/token"))
            .andRespond(withSuccess("{\"access_token\":\"access-token-abc\",\"expires_in\":300}", MediaType.APPLICATION_JSON));
        return new SapCcv2TokenProvider(restTemplate, new ObjectMapper(), "client-123", "secret-123",
            "https://auth.example.com/oauth2/token", "https://portalapi.commerce.ondemand.com");
    }

    private static byte[] gzip(String text) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return bytes.toByteArray();
    }
}
