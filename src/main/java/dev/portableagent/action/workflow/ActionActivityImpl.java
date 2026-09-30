package dev.portableagent.action.workflow;

import dev.portableagent.action.model.ActionStatus;
import dev.portableagent.action.service.ActionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mcp.gateway.enabled", havingValue = "true")
public class ActionActivityImpl implements ActionActivity {
    private final ActionService actionService;
    private final ActionCalls actionCalls;

    @Override
    public void run(UUID actionId, String payloadHash) {
        var action = actionService.start(actionId, payloadHash);
        if (action.getStatus() == ActionStatus.SUCCEEDED || action.getStatus() == ActionStatus.FAILED) {
            return;
        }
        var eventId = actionCalls.run(action);
        actionService.succeed(actionId, eventId);
    }

    @Override
    public void fail(UUID actionId) {
        actionService.fail(actionId);
    }
}
