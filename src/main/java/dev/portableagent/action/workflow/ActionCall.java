package dev.portableagent.action.workflow;

import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionKind;

public interface ActionCall {
    ActionKind kind();

    String run(Action action);
}
