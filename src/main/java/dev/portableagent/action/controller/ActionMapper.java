package dev.portableagent.action.controller;

import dev.portableagent.action.api.model.ActionDecisionRequest;
import dev.portableagent.action.api.model.ActionResponse;
import dev.portableagent.action.api.model.CalendarActionResult;
import dev.portableagent.action.api.model.ProposeActionRequest;
import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionDecision;
import dev.portableagent.action.service.CreateActionCommand;
import dev.portableagent.action.service.DecideActionCommand;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class ActionMapper {
  private ActionMapper() {}

  static CreateActionCommand toCommand(ProposeActionRequest request) {
    return new CreateActionCommand(
        request.getKind().getValue(),
        request.getConnector().getValue(),
        request.getPayload(),
        request.getRequestKey());
  }

  static DecideActionCommand toCommand(ActionDecisionRequest request) {
    return new DecideActionCommand(
        ActionDecision.valueOf(request.getDecision().getValue()), request.getPayloadHash());
  }

  static ActionResponse toResponse(Action action) {
    var response =
        new ActionResponse(
            action.getId(),
            ActionResponse.StatusEnum.fromValue(action.getStatus().name()),
            action.getKind(),
            action.getConnector(),
            action.getPayload(),
            action.getPayloadHash(),
            OffsetDateTime.ofInstant(action.getCreatedAt(), ZoneOffset.UTC),
            OffsetDateTime.ofInstant(action.getUpdatedAt(), ZoneOffset.UTC));
    if (action.getResult() != null) {
      response.setResult(new CalendarActionResult(action.getResult().eventId()));
    }
    return response;
  }
}
