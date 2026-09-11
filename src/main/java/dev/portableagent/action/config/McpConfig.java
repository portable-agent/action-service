package dev.portableagent.action.config;

import dev.portableagent.action.client.McpClient;
import dev.portableagent.action.client.McpToken;
import dev.portableagent.action.client.OidcMcpToken;
import dev.portableagent.action.client.RestMcpClient;
import java.net.http.HttpClient;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "mcp.gateway.enabled", havingValue = "true")
@EnableConfigurationProperties(McpProperties.class)
public class McpConfig {
    @Bean
    McpToken mcpToken(RestClient.Builder builder, McpProperties properties, Clock clock) {
        var restClient = client(builder, properties)
                .baseUrl(properties.tokenUrl().toString())
                .build();
        return new OidcMcpToken(
                restClient,
                properties.clientId(),
                properties.clientSecret(),
                properties.scope(),
                properties.tenantId(),
                clock);
    }

    @Bean
    McpClient mcpClient(RestClient.Builder builder, McpProperties properties, McpToken token) {
        var restClient =
                client(builder, properties).baseUrl(properties.url().toString()).build();
        return new RestMcpClient(restClient, token);
    }

    private RestClient.Builder client(RestClient.Builder builder, McpProperties properties) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder.clone().requestFactory(requestFactory);
    }
}
