package dev.portableagent.action.exception;

import java.util.UUID;

public class ActionNotFound extends RuntimeException {
  public ActionNotFound(UUID id) {
    super("Action %s was not found".formatted(id));
  }
}
