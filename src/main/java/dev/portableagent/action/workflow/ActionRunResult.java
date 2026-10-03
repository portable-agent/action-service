package dev.portableagent.action.workflow;

import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionKind;
import dev.portableagent.action.model.ActionStatus;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record ActionRunResult(
        UUID actionId, ActionKind kind, String connector, ActionStatus status, Map<String, Object> output) {
    public ActionRunResult {
        Objects.requireNonNull(actionId, "actionId must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        if (connector == null || connector.isBlank()) {
            throw new IllegalArgumentException("connector must not be blank");
        }
        Objects.requireNonNull(status, "status must not be null");
        output = output == null ? Map.of() : Map.copyOf(output);
    }

    public static ActionRunResult from(Action action) {
        var output = action.getResult() == null
                ? Map.<String, Object>of()
                : Map.<String, Object>of("eventId", action.getResult().eventId());
        return new ActionRunResult(
                action.getId(), ActionKind.from(action.getKind()), action.getConnector(), action.getStatus(), output);
    }

    public static ActionRunResult cancelled(ActionWorkflowInput input) {
        return new ActionRunResult(input.actionId(), input.kind(), input.connector(), ActionStatus.CANCELLED, Map.of());
    }
}
