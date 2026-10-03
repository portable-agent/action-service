package dev.portableagent.action.service;

import dev.portableagent.action.model.ActionKind;
import java.util.Map;

public interface ActionRule {
    ActionKind kind();

    void check(String connector, Map<String, Object> payload);
}
