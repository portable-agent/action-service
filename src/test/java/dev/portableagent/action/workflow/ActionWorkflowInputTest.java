package dev.portableagent.action.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionKind;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActionWorkflowInputTest {
    @Test
    void from_whenActionIsValid_shouldKeepSafeActionData() {
        var action = action();

        var input = ActionWorkflowInput.from(action);

        assertThat(input.actionId()).isEqualTo(action.getId());
        assertThat(input.tenantId()).isEqualTo(action.getTenantId());
        assertThat(input.actorId()).isEqualTo(action.getActorId());
        assertThat(input.requestKey()).isEqualTo(action.getRequestKey());
        assertThat(input.kind()).isEqualTo(ActionKind.CALENDAR_CREATE_EVENT);
        assertThat(input.connector()).isEqualTo("google-calendar");
        assertThat(input.payload()).containsEntry("title", "Demo");
        assertThat(input.payloadHash()).isEqualTo(action.getPayloadHash());
    }

    private Action action() {
        return Action.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "calendar-request-123",
                "calendar.create_event",
                "google-calendar",
                Map.of(
                        "title", "Demo",
                        "startAt", "2026-09-11T10:00:00+03:00",
                        "endAt", "2026-09-11T10:30:00+03:00",
                        "timeZone", "Europe/Moscow"),
                "a".repeat(64),
                Instant.parse("2026-09-11T06:00:00Z"));
    }
}
