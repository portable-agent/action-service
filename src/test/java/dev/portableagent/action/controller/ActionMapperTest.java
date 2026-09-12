package dev.portableagent.action.controller;

import static org.assertj.core.api.Assertions.assertThat;

import dev.portableagent.action.api.model.ActionDecisionRequest;
import dev.portableagent.action.api.model.CalendarCreateEventPayload;
import dev.portableagent.action.api.model.ProposeActionRequest;
import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionDecision;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActionMapperTest {
    @Test
    void toCommand_whenProposalIsValid_shouldMapGeneratedModel() {
        var payload = new CalendarCreateEventPayload(
                        "Demo",
                        OffsetDateTime.parse("2026-09-01T12:00:00+03:00"),
                        OffsetDateTime.parse("2026-09-01T12:30:00+03:00"),
                        "Europe/Moscow")
                .description("Planning")
                .attendees(Set.of("person@example.test"));
        var request = new ProposeActionRequest(
                ProposeActionRequest.KindEnum.CALENDAR_CREATE_EVENT,
                ProposeActionRequest.ConnectorEnum.FAKE_CALENDAR,
                payload,
                "request-123");

        var command = ActionMapper.toCommand(request);

        assertThat(command.kind()).isEqualTo("calendar.create_event");
        assertThat(command.connector()).isEqualTo("fake-calendar");
        assertThat(command.payload())
                .containsEntry("title", "Demo")
                .containsEntry("startAt", "2026-09-01T12:00+03:00")
                .containsEntry("endAt", "2026-09-01T12:30+03:00")
                .containsEntry("timeZone", "Europe/Moscow")
                .containsEntry("description", "Planning")
                .containsEntry("attendees", List.of("person@example.test"));
        assertThat(command.requestKey()).isEqualTo("request-123");
    }

    @Test
    void toCommand_whenDecisionIsValid_shouldMapGeneratedModel() {
        var request = new ActionDecisionRequest(ActionDecisionRequest.DecisionEnum.CONFIRM, "a".repeat(64));

        var command = ActionMapper.toCommand(request);

        assertThat(command.decision()).isEqualTo(ActionDecision.CONFIRM);
        assertThat(command.payloadHash()).isEqualTo("a".repeat(64));
    }

    @Test
    void toResponse_whenActionExists_shouldMapDomainModel() {
        var now = Instant.parse("2026-09-01T10:00:00Z");
        var action = Action.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "request-123",
                "calendar.create_event",
                "fake-calendar",
                validPayload(),
                "a".repeat(64),
                now);

        var response = ActionMapper.toResponse(action);

        assertThat(response.getId()).isEqualTo(action.getId());
        assertThat(response.getStatus().getValue()).isEqualTo("AWAITING_APPROVAL");
        assertThat(response.getPayload().getTitle()).isEqualTo("Demo");
        assertThat(response.getPayload().getStartAt().toString()).isEqualTo("2026-09-01T12:00+03:00");
        assertThat(response.getCreatedAt().toInstant()).isEqualTo(now);
    }

    @Test
    void toResponse_whenActionSucceeded_shouldMapEventId() {
        var now = Instant.parse("2026-09-01T10:00:00Z");
        var action = Action.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "request-123",
                "calendar.create_event",
                "fake-calendar",
                validPayload(),
                "a".repeat(64),
                now);
        action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), now.plusSeconds(1));
        action.startExecution(now.plusSeconds(2));
        action.succeed("event-123", now.plusSeconds(3));

        var response = ActionMapper.toResponse(action);

        assertThat(response.getStatus().getValue()).isEqualTo("SUCCEEDED");
        assertThat(response.getResult().getEventId()).isEqualTo("event-123");
    }

    private Map<String, Object> validPayload() {
        return Map.of(
                "title", "Demo",
                "startAt", "2026-09-01T12:00:00+03:00",
                "endAt", "2026-09-01T12:30:00+03:00",
                "timeZone", "Europe/Moscow");
    }
}
