package dev.portableagent.action.model;

import java.time.Instant;
import java.util.UUID;

public record OutboxItem(UUID id, UUID actionId, Instant createdAt, int attempts) {
  public static OutboxItem create(UUID actionId, Instant now) {
    return new OutboxItem(UUID.randomUUID(), actionId, now, 0);
  }
}
