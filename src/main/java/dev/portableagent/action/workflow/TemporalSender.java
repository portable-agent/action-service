package dev.portableagent.action.workflow;

import dev.portableagent.action.service.ActionService;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowStub;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TemporalSender {
    private final WorkflowClient workflowClient;
    private final ActionWorkflowOptions workflowOptions;
    private final ActionService actionService;

    public void send(UUID actionId) {
        var input = input(actionId);
        var workflow = newWorkflow(input);
        try {
            WorkflowClient.start(workflow::run, input);
        } catch (WorkflowExecutionAlreadyStarted ignored) {
            // The same id keeps retries safe.
        }
    }

    public void sendDecision(UUID actionId, String decision, String payloadHash) {
        var input = input(actionId);
        var workflow = newWorkflow(input);
        WorkflowStub.fromTyped(workflow)
                .signalWithStart("decision", new Object[] {decision, payloadHash}, new Object[] {input});
    }

    private ActionWorkflow newWorkflow(ActionWorkflowInput input) {
        return workflowClient.newWorkflowStub(ActionWorkflow.class, workflowOptions.make(input));
    }

    private ActionWorkflowInput input(UUID actionId) {
        return ActionWorkflowInput.from(actionService.getForWork(actionId));
    }
}
