package dev.portableagent.action.workflow;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ActionWorkflowV2 {
    @WorkflowMethod
    ActionRunResult run(ActionWorkflowInput input);

    @SignalMethod
    void decision(String decision, String payloadHash);
}
