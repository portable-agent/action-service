package dev.portableagent.action.workflow;

import dev.portableagent.action.client.McpClient;
import dev.portableagent.action.mcp.api.model.McpCallRequest;
import dev.portableagent.action.model.ActionStatus;
import dev.portableagent.action.service.ActionService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mcp.gateway.enabled", havingValue = "true")
public class ActionActivityImpl implements ActionActivity {
    private static final String CALENDAR_ACTION = "calendar.create_event";
    private static final String CREATE_EVENT = "create_event";

    private final ActionService actionService;
    private final McpClient mcpClient;

    public ActionActivityImpl(ActionService actionService, McpClient mcpClient) {
        this.actionService = actionService;
        this.mcpClient = mcpClient;
    }

    @Override
    public void run(UUID actionId, String payloadHash) {
        var action = actionService.start(actionId, payloadHash);
        if (action.getStatus() == ActionStatus.SUCCEEDED || action.getStatus() == ActionStatus.FAILED) {
            return;
        }
        if (!CALENDAR_ACTION.equals(action.getKind())) {
            throw new IllegalArgumentException("Action kind is not supported");
        }

        var request = new McpCallRequest(
                action.getId(),
                action.getConnector(),
                CREATE_EVENT,
                calendarInput(action.getPayload(), action.getRequestKey()),
                action.getRequestKey());
        var result = mcpClient.call(action.getTenantId(), request);
        actionService.succeed(actionId, result.eventId());
    }

    @Override
    public void fail(UUID actionId) {
        actionService.fail(actionId);
    }

    private Map<String, Object> calendarInput(Map<String, Object> payload, String requestKey) {
        var input = new LinkedHashMap<String, Object>();
        input.put("request_key", requestKey);
        input.put("title", payload.get("title"));
        input.put("start_at", payload.get("startAt"));
        input.put("end_at", payload.get("endAt"));
        input.put("time_zone", payload.get("timeZone"));
        copyOptional(payload, input, "description");
        copyOptional(payload, input, "attendees");
        return input;
    }

    private void copyOptional(Map<String, Object> payload, Map<String, Object> input, String name) {
        if (payload.containsKey(name)) {
            input.put(name, payload.get(name));
        }
    }
}
