package dev.portableagent.action.controller;

import dev.portableagent.action.api.model.ActionDecisionRequest;
import dev.portableagent.action.api.model.ActionResponse;
import dev.portableagent.action.api.model.CalendarActionResult;
import dev.portableagent.action.api.model.CalendarCreateEventPayload;
import dev.portableagent.action.api.model.ProposeActionRequest;
import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionDecision;
import dev.portableagent.action.service.CreateActionCommand;
import dev.portableagent.action.service.DecideActionCommand;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

final class ActionMapper {
    private ActionMapper() {}

    static CreateActionCommand toCommand(ProposeActionRequest request) {
        return new CreateActionCommand(
                request.getKind().getValue(),
                request.getConnector().getValue(),
                toMap(request.getPayload()),
                request.getRequestKey());
    }

    static DecideActionCommand toCommand(ActionDecisionRequest request) {
        return new DecideActionCommand(
                ActionDecision.valueOf(request.getDecision().getValue()), request.getPayloadHash());
    }

    static ActionResponse toResponse(Action action) {
        var response = new ActionResponse(
                action.getId(),
                ActionResponse.StatusEnum.fromValue(action.getStatus().name()),
                action.getKind(),
                action.getConnector(),
                toPayload(action.getPayload()),
                action.getPayloadHash(),
                OffsetDateTime.ofInstant(action.getCreatedAt(), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(action.getUpdatedAt(), ZoneOffset.UTC));
        if (action.getResult() != null) {
            response.setResult(new CalendarActionResult(action.getResult().eventId()));
        }
        return response;
    }

    private static Map<String, Object> toMap(CalendarCreateEventPayload payload) {
        var values = new LinkedHashMap<String, Object>();
        values.put("title", payload.getTitle());
        values.put("startAt", payload.getStartAt().toString());
        values.put("endAt", payload.getEndAt().toString());
        values.put("timeZone", payload.getTimeZone());
        if (payload.getDescription() != null) {
            values.put("description", payload.getDescription());
        }
        if (payload.getAttendees() != null && !payload.getAttendees().isEmpty()) {
            values.put("attendees", payload.getAttendees().stream().toList());
        }
        return values;
    }

    private static CalendarCreateEventPayload toPayload(Map<String, Object> values) {
        var payload = new CalendarCreateEventPayload(
                (String) values.get("title"),
                OffsetDateTime.parse((String) values.get("startAt")),
                OffsetDateTime.parse((String) values.get("endAt")),
                (String) values.get("timeZone"));
        if (values.get("description") instanceof String description) {
            payload.setDescription(description);
        }
        if (values.get("attendees") instanceof Iterable<?> attendees) {
            var emails = new LinkedHashSet<String>();
            attendees.forEach(email -> emails.add((String) email));
            payload.setAttendees(emails);
        }
        return payload;
    }
}
