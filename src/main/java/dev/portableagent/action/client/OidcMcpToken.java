package dev.portableagent.action.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class OidcMcpToken implements McpToken {
    private static final long REFRESH_BEFORE_SECONDS = 30;

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final UUID tenantId;
    private final Clock clock;

    private String value;
    private Instant expiresAt = Instant.EPOCH;

    public OidcMcpToken(
            RestClient restClient, String clientId, String clientSecret, String scope, UUID tenantId, Clock clock) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.tenantId = tenantId;
        this.clock = clock;
    }

    @Override
    public synchronized String get(UUID requestedTenantId) {
        if (!tenantId.equals(requestedTenantId)) {
            throw new McpCallFailed("Action tenant is not allowed for this worker");
        }
        var now = clock.instant();
        if (value != null && now.isBefore(expiresAt)) {
            return value;
        }

        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("scope", scope);

        TokenResponse response;
        try {
            response = restClient
                    .post()
                    .uri("")
                    .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RestClientException error) {
            throw new McpCallFailed("Cannot get gateway token");
        }
        if (response == null
                || response.accessToken() == null
                || response.accessToken().isBlank()) {
            throw new McpCallFailed("Gateway token response is invalid");
        }

        value = response.accessToken();
        expiresAt = now.plusSeconds(Math.max(1, response.expiresIn() - REFRESH_BEFORE_SECONDS));
        return value;
    }

    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {}
}
