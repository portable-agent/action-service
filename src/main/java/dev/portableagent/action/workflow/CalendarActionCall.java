package dev.portableagent.action.workflow;

import dev.portableagent.action.client.CalendarRequestMapper;
import dev.portableagent.action.client.McpClient;
import dev.portableagent.action.model.Action;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mcp.gateway.enabled", havingValue = "true")
public class CalendarActionCall implements ActionCall {
    private static final String KIND = "calendar.create_event";

    private final McpClient mcpClient;
    private final CalendarRequestMapper requestMapper;

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public String run(Action action) {
        var request = requestMapper.make(action);
        return mcpClient.call(action.getTenantId(), request).eventId();
    }
}
