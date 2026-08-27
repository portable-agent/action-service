package dev.portableagent.action.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActionProposalTest {
    private static final Instant NOW = Instant.parse("2026-08-28T10:00:00Z");

    @Test
    void decide_whenHashMatches_shouldApproveAction() {
        var action = action();

        action.decide(ActionDecision.CONFIRM, "a".repeat(64), NOW.plusSeconds(1));

        assertThat(action.getStatus()).isEqualTo(ActionStatus.APPROVED);
    }

    @Test
    void decide_whenHashDiffers_shouldRejectDecision() {
        var action = action();

        assertThatThrownBy(() -> action.decide(ActionDecision.CONFIRM, "b".repeat(64), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payload hash");
    }

    private ActionProposal action() {
        return ActionProposal.propose(UUID.randomUUID(), UUID.randomUUID(), "request-123", "calendar.create_event", "calendar", "a".repeat(64), NOW);
    }
}
