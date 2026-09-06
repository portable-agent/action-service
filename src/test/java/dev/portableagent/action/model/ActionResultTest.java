package dev.portableagent.action.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ActionResultTest {
  @Test
  void create_whenEventIdIsBlank_shouldRejectResult() {
    assertThatThrownBy(() -> new ActionResult(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("eventId");
  }

  @Test
  void create_whenEventIdIsTooLong_shouldRejectResult() {
    assertThatThrownBy(() -> new ActionResult("a".repeat(257)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("256");
  }
}
