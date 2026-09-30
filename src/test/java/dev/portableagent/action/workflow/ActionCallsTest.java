package dev.portableagent.action.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.portableagent.action.model.Action;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActionCallsTest {
    @Mock
    ActionCall calendarCall;

    @Mock
    ActionCall taskCall;

    @Test
    void run_whenKindExists_shouldUseMatchingCall() {
        var action = action("calendar.create_event");
        when(calendarCall.kind()).thenReturn("calendar.create_event");
        when(taskCall.kind()).thenReturn("task.create");
        when(calendarCall.run(action)).thenReturn("event-123");
        var calls = new ActionCalls(List.of(calendarCall, taskCall));

        var result = calls.run(action);

        assertThat(result).isEqualTo("event-123");
        verify(calendarCall).run(action);
        verify(taskCall, never()).run(any());
    }

    @Test
    void run_whenKindDoesNotExist_shouldRejectAction() {
        var action = action("payment.send");
        when(calendarCall.kind()).thenReturn("calendar.create_event");
        var calls = new ActionCalls(List.of(calendarCall));

        assertThatThrownBy(() -> calls.run(action))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported action kind: payment.send");
    }

    private Action action(String kind) {
        return Action.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "request-123",
                kind,
                "fake-calendar",
                Map.of("title", "Demo"),
                "a".repeat(64),
                Instant.parse("2026-09-11T06:00:00Z"));
    }
}
