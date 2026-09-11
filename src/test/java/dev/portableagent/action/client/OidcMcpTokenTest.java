package dev.portableagent.action.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadGateway;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OidcMcpTokenTest {
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MockRestServiceServer server;
    private OidcMcpToken token;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://keycloak:8080/token");
        server = MockRestServiceServer.bindTo(builder).build();
        token = new OidcMcpToken(
                builder.build(),
                "action-service",
                "test-secret",
                "mcp:call calendar:write",
                TENANT_ID,
                Clock.fixed(Instant.parse("2026-09-11T06:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void get_whenTokenIsValid_shouldReuseToken() {
        server.expect(once(), requestTo("http://keycloak:8080/token"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Basic YWN0aW9uLXNlcnZpY2U6dGVzdC1zZWNyZXQ="))
                .andExpect(content().string("grant_type=client_credentials&scope=mcp%3Acall+calendar%3Awrite"))
                .andRespond(withSuccess(
                        "{\"access_token\":\"service-token\",\"expires_in\":300}", MediaType.APPLICATION_JSON));

        assertThat(token.get(TENANT_ID)).isEqualTo("service-token");
        assertThat(token.get(TENANT_ID)).isEqualTo("service-token");
        server.verify();
    }

    @Test
    void get_whenTokenEndpointFails_shouldReturnSafeError() {
        server.expect(requestTo("http://keycloak:8080/token"))
                .andRespond(withBadGateway().body("private identity error"));

        assertThatThrownBy(() -> token.get(TENANT_ID))
                .isInstanceOf(McpCallFailed.class)
                .hasMessage("Cannot get gateway token")
                .hasNoCause()
                .message()
                .doesNotContain("private identity error");
    }

    @Test
    void get_whenTenantIsDifferent_shouldRejectBeforeTokenCall() {
        assertThatThrownBy(() -> token.get(UUID.fromString("22222222-2222-2222-2222-222222222222")))
                .isInstanceOf(McpCallFailed.class)
                .hasMessage("Action tenant is not allowed for this worker");
        server.verify();
    }
}
