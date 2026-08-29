package dev.portableagent.action.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActionTest {
  private static final Instant NOW = Instant.parse("2026-08-28T10:00:00Z");

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
        "a".repeat(64),
        NOW);
  }
}
