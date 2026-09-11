package dev.portableagent.action.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadGateway;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.portableagent.action.mcp.api.model.McpCallRequest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestMcpClientTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MockRestServiceServer server;
    private McpToken token;
    private RestMcpClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://mcp-gateway:8080");
        server = MockRestServiceServer.bindTo(builder).build();
        token = Mockito.mock(McpToken.class);
        when(token.get(TENANT_ID)).thenReturn("test-token");
        client = new RestMcpClient(builder.build(), token);
    }

    @Test
    void call_whenGatewaySucceeds_shouldReturnEventId() {
        var actionId = UUID.randomUUID();
        server.expect(once(), requestTo("http://mcp-gateway:8080/api/v1/calls"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(content().json("""
                        {
                          "actionId": "%s",
                          "connector": "fake-calendar",
                          "tool": "create_event",
                          "input": {"title": "Demo"},
                          "requestKey": "calendar-request-123"
                        }
                        """.formatted(actionId)))
                .andRespond(withSuccess("""
                        {"data":{"eventId":"event-123"}}
                        """, MediaType.APPLICATION_JSON));

        var result = client.call(
                TENANT_ID,
                new McpCallRequest(
                        actionId, "fake-calendar", "create_event", Map.of("title", "Demo"), "calendar-request-123"));

        assertThat(result.eventId()).isEqualTo("event-123");
        verify(token).get(TENANT_ID);
        server.verify();
    }

    @Test
    void call_whenGatewayFails_shouldReturnSafeError() {
        server.expect(requestTo("http://mcp-gateway:8080/api/v1/calls"))
                .andRespond(withBadGateway().body("private connector error"));

        assertThatThrownBy(() -> client.call(
                        TENANT_ID,
                        new McpCallRequest(
                                UUID.randomUUID(),
                                "fake-calendar",
                                "create_event",
                                Map.of("title", "Demo"),
                                "calendar-request-123")))
                .isInstanceOf(McpCallFailed.class)
                .hasMessage("Gateway call failed")
                .hasNoCause()
                .message()
                .doesNotContain("private connector error");
    }

    @Test
    void call_whenEventIdIsMissing_shouldRejectResponse() {
        server.expect(requestTo("http://mcp-gateway:8080/api/v1/calls"))
                .andRespond(withSuccess("{\"data\":{}}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.call(
                        TENANT_ID,
                        new McpCallRequest(
                                UUID.randomUUID(),
                                "fake-calendar",
                                "create_event",
                                Map.of("title", "Demo"),
                                "calendar-request-123")))
                .isInstanceOf(McpCallFailed.class)
                .hasMessage("Gateway response is invalid");
    }
}
