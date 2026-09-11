package dev.portableagent.action.client;

import static org.assertj.core.api.Assertions.assertThat;

import dev.portableagent.action.model.Action;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalendarRequestMapperTest {
    private final CalendarRequestMapper mapper = new CalendarRequestMapper();

    @Test
    void make_shouldMapPublicPayloadToCalendarToolInput() {
        var action = action(Map.of(
                "title", "Demo",
                "startAt", "2026-09-11T10:00:00+03:00",
                "endAt", "2026-09-11T10:30:00+03:00",
                "timeZone", "Europe/Moscow",
                "description", "Planning",
                "attendees", List.of("person@example.test"),
                "request_key", "untrusted-value",
                "unknown", "not-forwarded"));

        var request = mapper.make(action);

        assertThat(request.getInput())
                .containsEntry("request_key", action.getRequestKey())
                .containsEntry("title", "Demo")
                .containsEntry("start_at", "2026-09-11T10:00:00+03:00")
                .containsEntry("end_at", "2026-09-11T10:30:00+03:00")
                .containsEntry("time_zone", "Europe/Moscow")
                .containsEntry("description", "Planning")
                .containsEntry("attendees", List.of("person@example.test"))
                .doesNotContainKey("unknown");
        assertThat(request.getRequestKey()).isEqualTo(action.getRequestKey());
        assertThat(request.getTool()).isEqualTo("create_event");
    }

    private Action action(Map<String, Object> payload) {
        return Action.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "calendar-request-123",
                "calendar.create_event",
                "fake-calendar",
                payload,
                "a".repeat(64),
                Instant.parse("2026-09-11T06:00:00Z"));
    }
}
