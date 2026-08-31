package dev.portableagent.action.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActionTest {
  private static final Instant NOW = Instant.parse("2026-08-28T10:00:00Z");

  @Test
  void create_whenPayloadIsGiven_shouldKeepPayload() {
    var payload = Map.<String, Object>of("title", "Demo");

    var action =
        Action.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "request-123",
            "calendar.create_event",
            "fake-calendar",
            payload,
            "a".repeat(64),
            NOW);

    assertThat(action.getPayload()).isEqualTo(payload);
  }

  @Test
  void create_whenSourcePayloadChanges_shouldKeepOriginalPayload() {
    var attendees = new ArrayList<>(List.of("first@example.test"));
    var payload = new LinkedHashMap<String, Object>();
    payload.put("title", "Demo");
    payload.put("attendees", attendees);
    var action =
        Action.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "request-123",
            "calendar.create_event",
            "fake-calendar",
            payload,
            "a".repeat(64),
            NOW);

    payload.put("title", "Changed");
    attendees.add("second@example.test");

    assertThat(action.getPayload())
        .containsEntry("title", "Demo")
        .containsEntry("attendees", List.of("first@example.test"));
  }

  @Test
  void applyDecision_whenHashMatches_shouldApproveAction() {
    var action = action();

    action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), NOW.plusSeconds(1));

    assertThat(action.getStatus()).isEqualTo(ActionStatus.APPROVED);
  }

  @Test
  void applyDecision_whenHashDiffers_shouldRejectDecision() {
    var action = action();

    assertThatThrownBy(() -> action.applyDecision(ActionDecision.CONFIRM, "b".repeat(64), NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Payload hash");
  }

  private Action action() {
    return Action.create(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "request-123",
        "calendar.create_event",
        "calendar",
        Map.of("title", "Demo"),
        "a".repeat(64),
        NOW);
  }
}
