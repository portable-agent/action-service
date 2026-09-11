package dev.portableagent.action.model;

import java.time.Instant;
import java.util.UUID;

public record OutboxItem(
        UUID id, UUID actionId, OutboxType type, String decision, String payloadHash, Instant createdAt, int attempts) {
    public static OutboxItem start(UUID actionId, Instant now) {
        return new OutboxItem(UUID.randomUUID(), actionId, OutboxType.START, null, null, now, 0);
    }

    public static OutboxItem decision(UUID actionId, String decision, String payloadHash, Instant now) {
        return new OutboxItem(UUID.randomUUID(), actionId, OutboxType.DECISION, decision, payloadHash, now, 0);
    }
}
