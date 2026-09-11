package dev.portableagent.action.workflow;

import dev.portableagent.action.config.TemporalProperties;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TemporalSender {
    private final WorkflowClient workflowClient;
    private final TemporalProperties properties;

    public TemporalSender(WorkflowClient workflowClient, TemporalProperties properties) {
        this.workflowClient = workflowClient;
        this.properties = properties;
    }

    public void send(UUID actionId) {
        var workflow = newWorkflow(actionId);
        try {
            WorkflowClient.start(workflow::run, actionId);
        } catch (WorkflowExecutionAlreadyStarted ignored) {
            // The same id keeps retries safe.
        }
    }

    public void sendDecision(UUID actionId, String decision, String payloadHash) {
        var workflow = newWorkflow(actionId);
        WorkflowStub.fromTyped(workflow)
                .signalWithStart("decision", new Object[] {decision, payloadHash}, new Object[] {actionId});
    }

    private ActionWorkflow newWorkflow(UUID actionId) {
        return workflowClient.newWorkflowStub(
                ActionWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("action-" + actionId)
                        .setTaskQueue(properties.taskQueue())
                        .build());
    }
}
