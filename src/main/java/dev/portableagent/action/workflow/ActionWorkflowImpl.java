package dev.portableagent.action.workflow;

import dev.portableagent.action.model.ActionDecision;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.UUID;

public class ActionWorkflowImpl implements ActionWorkflow {
    private final ActionActivity activity = Workflow.newActivityStub(
            ActionActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumAttempts(3)
                            .build())
                    .build());

    private ActionDecision decision;
    private String payloadHash;

    @Override
    public void run(UUID actionId) {
        Workflow.await(() -> decision != null);
        switch (decision) {
            case CONFIRM -> runAction(actionId);
            case CANCEL -> {
                // The decision was already saved by ActionService.
            }
        }
    }

    private void runAction(UUID actionId) {
        try {
            activity.run(actionId, payloadHash);
        } catch (ActivityFailure error) {
            activity.fail(actionId);
        }
    }

    @Override
    public void decision(String newDecision, String newPayloadHash) {
        decision = ActionDecision.valueOf(newDecision);
        payloadHash = newPayloadHash;
    }
}
