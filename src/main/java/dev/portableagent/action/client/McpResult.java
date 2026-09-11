package dev.portableagent.action.client;

public record McpResult(String eventId) {
    public McpResult {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
    }
}
