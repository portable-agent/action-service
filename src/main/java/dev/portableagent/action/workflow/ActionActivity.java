package dev.portableagent.action.workflow;

import io.temporal.activity.ActivityInterface;

@ActivityInterface(namePrefix = "Action")
public interface ActionActivity {
    ActionRunResult run(ActionRunInput input);

    ActionRunResult fail(ActionWorkflowInput input);
}
