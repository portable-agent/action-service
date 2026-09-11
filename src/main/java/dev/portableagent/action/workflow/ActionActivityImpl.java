package dev.portableagent.action.workflow;

import dev.portableagent.action.client.CalendarRequestMapper;
import dev.portableagent.action.client.McpClient;
import dev.portableagent.action.model.ActionStatus;
import dev.portableagent.action.service.ActionService;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mcp.gateway.enabled", havingValue = "true")
public class ActionActivityImpl implements ActionActivity {
    private static final String CALENDAR_ACTION = "calendar.create_event";
    private final ActionService actionService;
    private final McpClient mcpClient;
    private final CalendarRequestMapper requestMapper;

    public ActionActivityImpl(ActionService actionService, McpClient mcpClient, CalendarRequestMapper requestMapper) {
        this.actionService = actionService;
        this.mcpClient = mcpClient;
        this.requestMapper = requestMapper;
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

        var request = requestMapper.make(action);
        var result = mcpClient.call(action.getTenantId(), request);
        actionService.succeed(actionId, result.eventId());
    }

    @Override
    public void fail(UUID actionId) {
        actionService.fail(actionId);
    }
}
