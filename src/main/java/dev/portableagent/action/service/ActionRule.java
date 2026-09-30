package dev.portableagent.action.service;

import java.util.Map;

public interface ActionRule {
    String kind();

    void check(String connector, Map<String, Object> payload);
}
