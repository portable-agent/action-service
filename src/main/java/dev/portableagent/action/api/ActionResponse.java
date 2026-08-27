package dev.portableagent.action.api;

import dev.portableagent.action.domain.ActionProposal;
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
    public static ActionResponse from(ActionProposal action) {
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
