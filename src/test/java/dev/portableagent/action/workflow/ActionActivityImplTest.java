package dev.portableagent.action.workflow;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.portableagent.action.client.McpCallFailed;
import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionDecision;
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
        when(actionService.start(action.getId(), action.getPayloadHash())).thenReturn(action);
        when(actionCalls.run(action)).thenReturn("event-123");

        activity.run(action.getId(), action.getPayloadHash());

        verify(actionCalls).run(action);
        verify(actionService).succeed(action.getId(), "event-123");
    }

    @Test
    void run_whenMcpCallFails_shouldLetTemporalRetry() {
        var action = approvedAction();
        when(actionService.start(action.getId(), action.getPayloadHash())).thenReturn(action);
        when(actionCalls.run(action)).thenThrow(new McpCallFailed("Gateway call failed"));

        assertThatThrownBy(() -> activity.run(action.getId(), action.getPayloadHash()))
                .isInstanceOf(McpCallFailed.class);

        verify(actionService, never()).fail(action.getId());
    }

    @Test
    void fail_shouldMarkActionFailed() {
        var actionId = UUID.randomUUID();

        activity.fail(actionId);

        verify(actionService).fail(actionId);
    }

    @Test
    void run_whenActionAlreadySucceeded_shouldNotCallMcpAgain() {
        var action = approvedAction();
        action.startExecution(Instant.parse("2026-09-11T06:02:00Z"));
        action.succeed("event-123", Instant.parse("2026-09-11T06:03:00Z"));
        when(actionService.start(action.getId(), action.getPayloadHash())).thenReturn(action);

        activity.run(action.getId(), action.getPayloadHash());

        verifyNoInteractions(actionCalls);
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
}
