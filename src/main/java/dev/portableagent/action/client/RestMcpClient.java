package dev.portableagent.action.client;

import java.util.Map;
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
    public McpResult call(McpRequest request) {
        McpResponse response;
        try {
            response = restClient
                    .post()
                    .uri("/api/v1/calls")
                    .headers(headers -> headers.setBearerAuth(token.get(request.tenantId())))
                    .body(GatewayRequest.from(request))
                    .retrieve()
                    .body(McpResponse.class);
        } catch (RestClientException error) {
            throw new McpCallFailed("Gateway call failed");
        }

        var eventId = response == null ? null : response.eventId();
        if (eventId == null || eventId.isBlank()) {
            throw new McpCallFailed("Gateway response is invalid");
        }
        return new McpResult(eventId);
    }

    record GatewayRequest(
            java.util.UUID actionId, String connector, String tool, Map<String, Object> input, String requestKey) {
        static GatewayRequest from(McpRequest request) {
            return new GatewayRequest(
                    request.actionId(), request.connector(), request.tool(), request.input(), request.requestKey());
        }
    }

    record McpResponse(Map<String, Object> data) {
        String eventId() {
            if (data == null) {
                return null;
            }
            var value = data.get("eventId");
            return value instanceof String text ? text : null;
        }
    }
}
