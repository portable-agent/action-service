package dev.portableagent.action.client;

import dev.portableagent.action.mcp.api.model.McpCallRequest;
import java.util.UUID;

public interface McpClient {
    McpResult call(UUID tenantId, McpCallRequest request);
}
