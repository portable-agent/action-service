package dev.portableagent.action.exception;

import java.util.UUID;

public class ActionChanged extends IllegalStateException {
    public ActionChanged(UUID id) {
        super("Action %s was changed by another request".formatted(id));
    }
}
