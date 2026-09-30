package dev.portableagent.action.workflow;

import dev.portableagent.action.model.Action;

public interface ActionCall {
    String kind();

    String run(Action action);
}
