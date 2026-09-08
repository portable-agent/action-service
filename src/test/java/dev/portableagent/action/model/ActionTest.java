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

        var action = Action.create(
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
        var action = Action.create(
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

        var changed = action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), NOW.plusSeconds(1));

        assertThat(changed).isTrue();
        assertThat(action.getStatus()).isEqualTo(ActionStatus.APPROVED);
    }

    @Test
    void applyDecision_whenSameConfirmIsRepeated_shouldKeepApprovedAction() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), NOW.plusSeconds(1));

        var changed = action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), NOW.plusSeconds(2));

        assertThat(changed).isFalse();
        assertThat(action.getStatus()).isEqualTo(ActionStatus.APPROVED);
        assertThat(action.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void applyDecision_whenConfirmIsRepeatedDuringExecution_shouldKeepExecutingAction() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), NOW.plusSeconds(1));
        action.startExecution(NOW.plusSeconds(2));

        var changed = action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), NOW.plusSeconds(3));

        assertThat(changed).isFalse();
        assertThat(action.getStatus()).isEqualTo(ActionStatus.EXECUTING);
        assertThat(action.getUpdatedAt()).isEqualTo(NOW.plusSeconds(2));
    }

    @Test
    void applyDecision_whenCancelIsGiven_shouldCancelAction() {
        var action = action();

        var changed = action.applyDecision(ActionDecision.CANCEL, "a".repeat(64), NOW.plusSeconds(1));

        assertThat(changed).isTrue();
        assertThat(action.getStatus()).isEqualTo(ActionStatus.CANCELLED);
    }

    @Test
    void applyDecision_whenDecisionChanges_shouldRejectDecision() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), NOW.plusSeconds(1));

        assertThatThrownBy(() -> action.applyDecision(ActionDecision.CANCEL, "a".repeat(64), NOW.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("waiting for approval");
    }

    @Test
    void applyDecision_whenHashDiffers_shouldRejectDecision() {
        var action = action();

        assertThatThrownBy(() -> action.applyDecision(ActionDecision.CONFIRM, "b".repeat(64), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payload hash");
    }

    @Test
    void succeed_whenActionIsExecuting_shouldStoreEventId() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), NOW.plusSeconds(1));
        action.startExecution(NOW.plusSeconds(2));

        var changed = action.succeed("event-123", NOW.plusSeconds(3));

        assertThat(changed).isTrue();
        assertThat(action.getStatus()).isEqualTo(ActionStatus.SUCCEEDED);
        assertThat(action.getResult()).isEqualTo(new ActionResult("event-123"));
    }

    @Test
    void succeed_whenSameResultIsRepeated_shouldKeepSucceededAction() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), NOW.plusSeconds(1));
        action.startExecution(NOW.plusSeconds(2));
        action.succeed("event-123", NOW.plusSeconds(3));

        var changed = action.succeed("event-123", NOW.plusSeconds(4));

        assertThat(changed).isFalse();
        assertThat(action.getUpdatedAt()).isEqualTo(NOW.plusSeconds(3));
    }

    @Test
    void succeed_whenResultDiffers_shouldRejectResult() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), NOW.plusSeconds(1));
        action.startExecution(NOW.plusSeconds(2));
        action.succeed("event-123", NOW.plusSeconds(3));

        assertThatThrownBy(() -> action.succeed("event-456", NOW.plusSeconds(4)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different result");
    }

    @Test
    void fail_whenActionIsExecuting_shouldMarkFailed() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), NOW.plusSeconds(1));
        action.startExecution(NOW.plusSeconds(2));

        var changed = action.fail(NOW.plusSeconds(3));

        assertThat(changed).isTrue();
        assertThat(action.getStatus()).isEqualTo(ActionStatus.FAILED);
        assertThat(action.getResult()).isNull();
    }

    @Test
    void fail_whenAlreadyFailed_shouldKeepFailedAction() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), NOW.plusSeconds(1));
        action.startExecution(NOW.plusSeconds(2));
        action.fail(NOW.plusSeconds(3));

        var changed = action.fail(NOW.plusSeconds(4));

        assertThat(changed).isFalse();
        assertThat(action.getUpdatedAt()).isEqualTo(NOW.plusSeconds(3));
    }

    @Test
    void succeed_whenActionIsNotExecuting_shouldRejectResult() {
        var action = action();

        assertThatThrownBy(() -> action.succeed("event-123", NOW.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("executing");
    }

    @Test
    void startExecution_whenActionIsNotApproved_shouldRejectStart() {
        var action = action();

        assertThatThrownBy(() -> action.startExecution(NOW.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approved");
    }

    @Test
    void fail_whenActionIsNotExecuting_shouldRejectFailure() {
        var action = action();
        action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), NOW.plusSeconds(1));

        assertThatThrownBy(() -> action.fail(NOW.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("executing");
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
