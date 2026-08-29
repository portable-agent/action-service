package dev.portableagent.action.dto;

import dev.portableagent.action.model.Action;
import java.time.Instant;
import java.util.UUID;

public record ActionResponse(
    UUID id,
    String status,
    String kind,
    String connector,
    String payloadHash,
    Instant createdAt,
    Instant updatedAt) {
  public static ActionResponse from(Action action) {
    return new ActionResponse(
        action.getId(),
        action.getStatus().name(),
        action.getKind(),
        action.getConnector(),
        action.getPayloadHash(),
        action.getCreatedAt(),
        action.getUpdatedAt());
  }
}
