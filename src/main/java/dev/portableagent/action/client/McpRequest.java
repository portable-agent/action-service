package dev.portableagent.action.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record McpRequest(
        UUID actionId, UUID tenantId, String connector, String tool, Map<String, Object> input, String requestKey) {
    public McpRequest {
        Objects.requireNonNull(actionId);
        Objects.requireNonNull(tenantId);
        connector = requireText(connector, "connector");
        tool = requireText(tool, "tool");
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("input must not be empty");
        }
        input = Collections.unmodifiableMap(new LinkedHashMap<>(input));
        requestKey = requireText(requestKey, "requestKey");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
