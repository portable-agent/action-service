package dev.portableagent.action.client;

import java.util.UUID;

@FunctionalInterface
public interface McpToken {
    String get(UUID tenantId);
}
