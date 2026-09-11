package dev.portableagent.action.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("mcp.gateway")
public record McpProperties(
        @NotNull URI url,
        @NotNull URI tokenUrl,
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotBlank String scope,
        @NotNull UUID tenantId,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {}
