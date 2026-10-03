package dev.portableagent.action.workflow;

import io.temporal.activity.ActivityInterface;

@ActivityInterface(namePrefix = "ActionV2")
public interface ActionActivityV2 {
    ActionRunResult run(ActionRunInput input);

    ActionRunResult fail(ActionWorkflowInput input);
}
