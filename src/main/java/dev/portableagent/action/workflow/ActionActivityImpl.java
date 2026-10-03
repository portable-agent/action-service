package dev.portableagent.action.workflow;

import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionStatus;
import dev.portableagent.action.service.ActionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mcp.gateway.enabled", havingValue = "true")
public class ActionActivityImpl implements ActionActivity, ActionActivityV2 {
    private final ActionService actionService;
    private final ActionCalls actionCalls;

    @Override
    public void run(UUID actionId, String payloadHash) {
        runAction(actionId, payloadHash);
    }

    @Override
    public void fail(UUID actionId) {
        actionService.fail(actionId);
    }

    @Override
    public ActionRunResult run(ActionRunInput input) {
        return ActionRunResult.from(runAction(input.action().actionId(), input.approvedPayloadHash()));
    }

    @Override
    public ActionRunResult fail(ActionWorkflowInput input) {
        return ActionRunResult.from(actionService.fail(input.actionId()));
    }

    private Action runAction(UUID actionId, String payloadHash) {
        var action = actionService.start(actionId, payloadHash);
        if (action.getStatus() == ActionStatus.SUCCEEDED || action.getStatus() == ActionStatus.FAILED) {
            return action;
        }
        var eventId = actionCalls.run(action);
        return actionService.succeed(action.getId(), eventId);
    }
}
