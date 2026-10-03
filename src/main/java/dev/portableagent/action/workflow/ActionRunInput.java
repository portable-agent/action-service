package dev.portableagent.action.workflow;

import java.util.Objects;

public record ActionRunInput(ActionWorkflowInput action, String approvedPayloadHash) {
    public ActionRunInput {
        Objects.requireNonNull(action, "action must not be null");
        if (approvedPayloadHash == null || approvedPayloadHash.isBlank()) {
            throw new IllegalArgumentException("approvedPayloadHash must not be blank");
        }
    }
}
