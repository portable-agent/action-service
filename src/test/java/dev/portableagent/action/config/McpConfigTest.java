package dev.portableagent.action.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.portableagent.action.client.McpClient;
import dev.portableagent.action.client.McpToken;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class McpConfigTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(McpConfig.class)
            .withBean(Clock.class, Clock::systemUTC);

    @Test
    void config_whenGatewayIsDisabled_shouldNotCreateClients() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(McpClient.class);
            assertThat(context).doesNotHaveBean(McpToken.class);
        });
    }

    @Test
    void config_whenGatewayIsEnabled_shouldCreateClients() {
        contextRunner
                .withPropertyValues(
                        "mcp.gateway.enabled=true",
                        "mcp.gateway.url=http://mcp-gateway:8080",
                        "mcp.gateway.token-url=http://keycloak:8080/token",
                        "mcp.gateway.client-id=action-service",
                        "mcp.gateway.client-secret=test-secret",
                        "mcp.gateway.scope=mcp:call calendar:write",
                        "mcp.gateway.tenant-id=11111111-1111-1111-1111-111111111111",
                        "mcp.gateway.connect-timeout=3s",
                        "mcp.gateway.read-timeout=10s")
                .run(context -> {
                    assertThat(context).hasSingleBean(McpClient.class);
                    assertThat(context).hasSingleBean(McpToken.class);
                });
    }
}
