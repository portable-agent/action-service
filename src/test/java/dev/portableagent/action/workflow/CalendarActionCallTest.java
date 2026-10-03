package dev.portableagent.action.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.portableagent.action.client.CalendarRequestMapper;
import dev.portableagent.action.client.McpClient;
import dev.portableagent.action.client.McpResult;
import dev.portableagent.action.mcp.api.model.McpCallRequest;
import dev.portableagent.action.model.Action;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarActionCallTest {
    @Mock
    McpClient mcpClient;

    @Mock
    CalendarRequestMapper requestMapper;

    @Mock
    McpCallRequest request;

    @Test
    void run_whenGatewaySucceeds_shouldReturnEventId() {
        var action = action();
        when(requestMapper.make(action)).thenReturn(request);
        when(mcpClient.call(action.getTenantId(), request)).thenReturn(new McpResult("event-123"));
        var call = new CalendarActionCall(mcpClient, requestMapper);

        var result = call.run(action);

        assertThat(result).isEqualTo("event-123");
        verify(mcpClient).call(action.getTenantId(), request);
    }

    private Action action() {
        return Action.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "request-123",
                "calendar.create_event",
                "fake-calendar",
                Map.of("title", "Demo"),
                "a".repeat(64),
                Instant.parse("2026-09-11T06:00:00Z"));
    }
}
