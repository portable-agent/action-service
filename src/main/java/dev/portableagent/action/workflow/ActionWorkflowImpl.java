package dev.portableagent.action.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.UUID;

public class ActionWorkflowImpl implements ActionWorkflow {
    private final ActionActivity activity = Workflow.newActivityStub(
            ActionActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .build());

    private String decision;
    private String payloadHash;

    @Override
    public void run(UUID actionId) {
        Workflow.await(() -> decision != null);
        if ("CONFIRM".equals(decision)) {
            activity.run(actionId, payloadHash);
        }
    }

    @Override
    public void decision(String newDecision, String newPayloadHash) {
        decision = newDecision;
        payloadHash = newPayloadHash;
    }
}
