package dev.portableagent.action.controller;

import static org.assertj.core.api.Assertions.assertThat;

import dev.portableagent.action.api.model.ActionDecisionRequest;
import dev.portableagent.action.api.model.ProposeActionRequest;
import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionDecision;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActionMapperTest {
  @Test
  void toCommand_whenProposalIsValid_shouldMapGeneratedModel() {
    var payload = Map.<String, Object>of("title", "Demo");
    var request =
        new ProposeActionRequest(
            ProposeActionRequest.KindEnum.CALENDAR_CREATE_EVENT,
            ProposeActionRequest.ConnectorEnum.FAKE_CALENDAR,
            payload,
            "request-123");

    var command = ActionMapper.toCommand(request);

    assertThat(command.kind()).isEqualTo("calendar.create_event");
    assertThat(command.connector()).isEqualTo("fake-calendar");
    assertThat(command.payload()).isEqualTo(payload);
    assertThat(command.requestKey()).isEqualTo("request-123");
  }

  @Test
  void toCommand_whenDecisionIsValid_shouldMapGeneratedModel() {
    var request =
        new ActionDecisionRequest(ActionDecisionRequest.DecisionEnum.CONFIRM, "a".repeat(64));

    var command = ActionMapper.toCommand(request);

    assertThat(command.decision()).isEqualTo(ActionDecision.CONFIRM);
    assertThat(command.payloadHash()).isEqualTo("a".repeat(64));
  }

  @Test
  void toResponse_whenActionExists_shouldMapDomainModel() {
    var now = Instant.parse("2026-09-01T10:00:00Z");
    var action =
        Action.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "request-123",
            "calendar.create_event",
            "fake-calendar",
            Map.of("title", "Demo"),
            "a".repeat(64),
            now);

    var response = ActionMapper.toResponse(action);

    assertThat(response.getId()).isEqualTo(action.getId());
    assertThat(response.getStatus().getValue()).isEqualTo("AWAITING_APPROVAL");
    assertThat(response.getPayload()).isEqualTo(action.getPayload());
    assertThat(response.getCreatedAt().toInstant()).isEqualTo(now);
  }
}
