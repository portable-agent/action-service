package dev.portableagent.action.client;

import dev.portableagent.action.mcp.api.model.McpCallRequest;
import dev.portableagent.action.mcp.api.model.McpCallResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class RestMcpClient implements McpClient {
    private final RestClient restClient;
    private final McpToken token;

    public RestMcpClient(RestClient restClient, McpToken token) {
        this.restClient = restClient;
        this.token = token;
    }

    @Override
    public McpResult call(java.util.UUID tenantId, McpCallRequest request) {
        McpCallResponse response;
        try {
            response = restClient
                    .post()
                    .uri("/api/v1/calls")
                    .headers(headers -> headers.setBearerAuth(token.get(tenantId)))
                    .body(request)
                    .retrieve()
                    .body(McpCallResponse.class);
        } catch (RestClientException error) {
            throw new McpCallFailed("Gateway call failed");
        }

        var eventId = eventId(response);
        if (eventId == null || eventId.isBlank()) {
            throw new McpCallFailed("Gateway response is invalid");
        }
        return new McpResult(eventId);
    }

    private String eventId(McpCallResponse response) {
        if (response == null || response.getData() == null) {
            return null;
        }
        var value = response.getData().get("eventId");
        return value instanceof String text ? text : null;
    }
}
