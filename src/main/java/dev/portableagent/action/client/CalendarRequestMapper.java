package dev.portableagent.action.client;

import dev.portableagent.action.mcp.api.model.McpCallRequest;
import dev.portableagent.action.model.Action;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CalendarRequestMapper {
    private static final String CREATE_EVENT = "create_event";

    public McpCallRequest make(Action action) {
        var input = new LinkedHashMap<String, Object>();
        var payload = action.getPayload();
        input.put("request_key", action.getRequestKey());
        input.put("title", payload.get("title"));
        input.put("start_at", payload.get("startAt"));
        input.put("end_at", payload.get("endAt"));
        input.put("time_zone", payload.get("timeZone"));
        copyOptional(payload, input, "description");
        copyOptional(payload, input, "attendees");
        return new McpCallRequest(action.getId(), action.getConnector(), CREATE_EVENT, input, action.getRequestKey());
    }

    private void copyOptional(Map<String, Object> payload, Map<String, Object> input, String name) {
        if (payload.containsKey(name)) {
            input.put(name, payload.get(name));
        }
    }
}
