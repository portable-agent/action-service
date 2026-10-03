package dev.portableagent.action.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.portableagent.action.client.McpCallFailed;
import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionDecision;
import dev.portableagent.action.model.ActionStatus;
import dev.portableagent.action.service.ActionService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActionActivityImplTest {
    @Mock
    ActionService actionService;

    @Mock
    ActionCalls actionCalls;

    private ActionActivityImpl activity;

    @BeforeEach
    void setUp() {
        activity = new ActionActivityImpl(actionService, actionCalls);
    }

    @Test
    void run_whenMcpCallSucceeds_shouldSaveEventId() {
        var action = approvedAction();
        var input = runInput(action);
        when(actionService.start(action.getId(), action.getPayloadHash())).thenAnswer(ignored -> {
            action.startExecution(Instant.parse("2026-09-11T06:02:00Z"));
            return action;
        });
        when(actionCalls.run(action)).thenReturn("event-123");
        when(actionService.succeed(action.getId(), "event-123")).thenAnswer(ignored -> {
            action.succeed("event-123", Instant.parse("2026-09-11T06:03:00Z"));
            return action;
        });

        var result = activity.run(input);

        verify(actionCalls).run(action);
        verify(actionService).succeed(action.getId(), "event-123");
        assertThat(result.status()).isEqualTo(ActionStatus.SUCCEEDED);
        assertThat(result.output()).containsEntry("eventId", "event-123");
    }

    @Test
    void run_whenMcpCallFails_shouldLetTemporalRetry() {
        var action = approvedAction();
        var input = runInput(action);
        when(actionService.start(action.getId(), action.getPayloadHash())).thenAnswer(ignored -> {
            action.startExecution(Instant.parse("2026-09-11T06:02:00Z"));
            return action;
        });
        when(actionCalls.run(action)).thenThrow(new McpCallFailed("Gateway call failed"));

        assertThatThrownBy(() -> activity.run(input)).isInstanceOf(McpCallFailed.class);

        verify(actionService, never()).fail(action.getId());
    }

    @Test
    void fail_shouldMarkActionFailed() {
        var action = approvedAction();
        action.startExecution(Instant.parse("2026-09-11T06:02:00Z"));
        when(actionService.fail(action.getId())).thenAnswer(ignored -> {
            action.fail(Instant.parse("2026-09-11T06:03:00Z"));
            return action;
        });

        var result = activity.fail(ActionWorkflowInput.from(action));

        verify(actionService).fail(action.getId());
        assertThat(result.status()).isEqualTo(ActionStatus.FAILED);
    }

    @Test
    void run_whenActionAlreadySucceeded_shouldNotCallMcpAgain() {
        var action = approvedAction();
        action.startExecution(Instant.parse("2026-09-11T06:02:00Z"));
        action.succeed("event-123", Instant.parse("2026-09-11T06:03:00Z"));
        when(actionService.start(action.getId(), action.getPayloadHash())).thenReturn(action);

        var result = activity.run(runInput(action));

        verifyNoInteractions(actionCalls);
        assertThat(result.status()).isEqualTo(ActionStatus.SUCCEEDED);
    }

    private Action approvedAction() {
        var action = Action.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "calendar-request-123",
                "calendar.create_event",
                "fake-calendar",
                Map.of(
                        "title", "Demo",
                        "startAt", "2026-09-11T10:00:00+03:00",
                        "endAt", "2026-09-11T10:30:00+03:00",
                        "timeZone", "Europe/Moscow"),
                "a".repeat(64),
                Instant.parse("2026-09-11T06:00:00Z"));
        action.applyDecision(ActionDecision.CONFIRM, action.getPayloadHash(), Instant.parse("2026-09-11T06:01:00Z"));
        return action;
    }

    private ActionRunInput runInput(Action action) {
        return new ActionRunInput(ActionWorkflowInput.from(action), action.getPayloadHash());
    }
}
