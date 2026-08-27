package dev.portableagent.action.application;

import java.util.UUID;

public class ActionNotFoundException extends RuntimeException {
    public ActionNotFoundException(UUID id) {
        super("Action %s was not found".formatted(id));
    }
}
