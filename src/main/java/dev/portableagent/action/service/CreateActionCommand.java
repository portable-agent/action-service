package dev.portableagent.action.service;

import java.util.Map;

public record CreateActionCommand(
    String kind, String connector, Map<String, Object> payload, String requestKey) {}
