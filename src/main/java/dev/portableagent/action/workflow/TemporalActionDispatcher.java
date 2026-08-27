package dev.portableagent.action.workflow;

import dev.portableagent.action.config.TemporalProperties;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TemporalActionDispatcher {
    private final WorkflowClient workflowClient;
    private final TemporalProperties properties;

    public TemporalActionDispatcher(WorkflowClient workflowClient, TemporalProperties properties) {
        this.workflowClient = workflowClient;
        this.properties = properties;
    }

    public void dispatch(UUID actionId) {
        var workflow = workflowClient.newWorkflowStub(
                ActionWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("action-" + actionId)
                        .setTaskQueue(properties.taskQueue())
                        .build());
        try {
            WorkflowClient.start(workflow::run, actionId);
        } catch (WorkflowExecutionAlreadyStarted ignored) {
            // Stable workflow id makes an outbox retry idempotent.
        }
    }
}
