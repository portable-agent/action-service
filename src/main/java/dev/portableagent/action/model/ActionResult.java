package dev.portableagent.action.model;

public record ActionResult(String eventId) {
  public ActionResult {
    if (eventId == null || eventId.isBlank()) {
      throw new IllegalArgumentException("eventId must not be blank");
    }
    if (eventId.length() > 256) {
      throw new IllegalArgumentException("eventId must not be longer than 256 characters");
    }
  }
}
