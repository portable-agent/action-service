package dev.portableagent.action.workflow;

import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionKind;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record ActionWorkflowInput(
        UUID actionId,
        UUID tenantId,
        UUID actorId,
        String requestKey,
        ActionKind kind,
        String connector,
        Map<String, Object> payload,
        String payloadHash) {
    public ActionWorkflowInput {
        Objects.requireNonNull(actionId, "actionId must not be null");
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(actorId, "actorId must not be null");
        requestKey = requireText(requestKey, "requestKey");
        Objects.requireNonNull(kind, "kind must not be null");
        connector = requireText(connector, "connector");
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("payload must not be empty");
        }
        payload = Map.copyOf(payload);
        payloadHash = requireText(payloadHash, "payloadHash");
    }

    public static ActionWorkflowInput from(Action action) {
        Objects.requireNonNull(action, "action must not be null");
        return new ActionWorkflowInput(
                action.getId(),
                action.getTenantId(),
                action.getActorId(),
                action.getRequestKey(),
                ActionKind.from(action.getKind()),
                action.getConnector(),
                action.getPayload(),
                action.getPayloadHash());
    }

    public String summary() {
        return kind.value() + " · " + connector;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
