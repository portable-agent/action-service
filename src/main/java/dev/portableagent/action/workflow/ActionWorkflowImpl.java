package dev.portableagent.action.workflow;

import dev.portableagent.action.model.ActionDecision;
import dev.portableagent.action.model.ActionStatus;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class ActionWorkflowImpl implements ActionWorkflow {
    private ActionDecision decision;
    private String payloadHash;

    @Override
    public ActionRunResult run(ActionWorkflowInput input) {
        Workflow.await(() -> decision != null);
        return switch (decision) {
            case CONFIRM -> runAction(input);
            case CANCEL -> cancel(input);
        };
    }

    private ActionRunResult runAction(ActionWorkflowInput input) {
        changeStatus(ActionStatus.EXECUTING);
        try {
            var result = activity("Run " + input.summary()).run(new ActionRunInput(input, payloadHash));
            changeStatus(result.status());
            if (result.status() == ActionStatus.FAILED) {
                throw actionFailed(input);
            }
            return result;
        } catch (ActivityFailure error) {
            activity("Mark " + input.kind().value() + " as failed").fail(input);
            changeStatus(ActionStatus.FAILED);
            throw ApplicationFailure.newNonRetryableFailureWithCause(
                    "Action " + input.actionId() + " failed", "ActionFailed", error);
        }
    }

    private ActionRunResult cancel(ActionWorkflowInput input) {
        changeStatus(ActionStatus.CANCELLED);
        return ActionRunResult.cancelled(input);
    }

    private ApplicationFailure actionFailed(ActionWorkflowInput input) {
        return ApplicationFailure.newNonRetryableFailure("Action " + input.actionId() + " failed", "ActionFailed");
    }

    private void changeStatus(ActionStatus status) {
        Workflow.upsertTypedSearchAttributes(ActionWorkflowOptions.ACTION_STATUS.valueSet(status.name()));
    }

    private ActionActivity activity(String summary) {
        return Workflow.newActivityStub(
                ActionActivity.class,
                ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(30))
                        .setRetryOptions(RetryOptions.newBuilder()
                                .setInitialInterval(Duration.ofSeconds(1))
                                .setMaximumAttempts(3)
                                .build())
                        .setSummary(summary)
                        .build());
    }

    @Override
    public void decision(String newDecision, String newPayloadHash) {
        decision = ActionDecision.valueOf(newDecision);
        payloadHash = newPayloadHash;
    }
}
