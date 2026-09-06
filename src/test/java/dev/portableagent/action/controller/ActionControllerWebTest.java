package dev.portableagent.action.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.portableagent.action.config.SecurityConfig;
import dev.portableagent.action.model.Action;
import dev.portableagent.action.model.ActionDecision;
import dev.portableagent.action.service.ActionService;
import dev.portableagent.action.service.CreateActionCommand;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ActionController.class)
@Import(SecurityConfig.class)
class ActionControllerWebTest {
  @Autowired MockMvc mockMvc;

  @MockitoBean ActionService actionService;
  @MockitoBean JwtDecoder jwtDecoder;

  @Test
  void proposeAction_whenRequestIsValid_shouldReturnJson() throws Exception {
    var tenantId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var action =
        Action.create(
            tenantId,
            userId,
            "request-123",
            "calendar.create_event",
            "fake-calendar",
            Map.of("title", "Demo"),
            "a".repeat(64),
            Instant.parse("2026-09-01T10:00:00Z"));
    when(actionService.create(eq(tenantId), eq(userId), any(CreateActionCommand.class)))
        .thenReturn(action);

    mockMvc
        .perform(
            post("/api/v1/actions")
                .with(
                    jwt()
                        .jwt(
                            token ->
                                token
                                    .subject(userId.toString())
                                    .claim("tenant_id", tenantId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "kind": "calendar.create_event",
                      "connector": "fake-calendar",
                      "payload": {"title": "Demo"},
                      "requestKey": "request-123"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/actions/" + action.getId()))
        .andExpect(jsonPath("$.payload.title").value("Demo"))
        .andExpect(jsonPath("$.result").doesNotExist())
        .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"));
  }

  @Test
  void proposeAction_whenRequestIsInvalid_shouldReturnBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/actions")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "kind": "calendar.create_event",
                      "connector": "fake-calendar",
                      "payload": {"title": "Demo"}
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.type").value("https://portable-agent.dev/problems/validation-failed"));
  }

  @Test
  void proposeAction_withoutJwt_shouldReturnUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "kind": "calendar.create_event",
                      "connector": "fake-calendar",
                      "payload": {"title": "Demo"},
                      "requestKey": "request-123"
                    }
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getAction_whenActionSucceeded_shouldReturnEventId() throws Exception {
    var tenantId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var now = Instant.parse("2026-09-01T10:00:00Z");
    var action =
        Action.create(
            tenantId,
            userId,
            "request-result",
            "calendar.create_event",
            "fake-calendar",
            Map.of("title", "Demo"),
            "a".repeat(64),
            now);
    action.applyDecision(ActionDecision.CONFIRM, "a".repeat(64), now.plusSeconds(1));
    action.startExecution(now.plusSeconds(2));
    action.succeed("event-123", now.plusSeconds(3));
    when(actionService.get(tenantId, action.getId())).thenReturn(action);

    mockMvc
        .perform(
            get("/api/v1/actions/{actionId}", action.getId())
                .with(
                    jwt()
                        .jwt(
                            token ->
                                token
                                    .subject(userId.toString())
                                    .claim("tenant_id", tenantId.toString()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCEEDED"))
        .andExpect(jsonPath("$.result.eventId").value("event-123"));
  }
}
