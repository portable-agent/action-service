package dev.portableagent.action.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.portableagent.action.api.model.ProposeActionRequest;
import dev.portableagent.action.model.Action;
import dev.portableagent.action.service.ActionService;
import dev.portableagent.action.service.CreateActionCommand;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class ActionControllerTest {
  @Mock ActionService actionService;

  @AfterEach
  void clearSecurity() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void proposeAction_whenJwtIsValid_shouldCallService() {
    var tenantId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var payload = Map.<String, Object>of("title", "Demo");
    var request =
        new ProposeActionRequest(
            ProposeActionRequest.KindEnum.CALENDAR_CREATE_EVENT,
            ProposeActionRequest.ConnectorEnum.FAKE_CALENDAR,
            payload,
            "request-123");
    var action =
        Action.create(
            tenantId,
            userId,
            "request-123",
            "calendar.create_event",
            "fake-calendar",
            payload,
            "a".repeat(64),
            Instant.parse("2026-09-01T10:00:00Z"));
    setJwt(tenantId, userId);
    var command = ArgumentCaptor.forClass(CreateActionCommand.class);
    when(actionService.create(eq(tenantId), eq(userId), command.capture())).thenReturn(action);

    var response = new ActionController(actionService).proposeAction(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getPayload()).isEqualTo(payload);
    assertThat(command.getValue().requestKey()).isEqualTo("request-123");
    verify(actionService).create(eq(tenantId), eq(userId), eq(command.getValue()));
  }

  private void setJwt(UUID tenantId, UUID userId) {
    var jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(userId.toString())
            .claim("tenant_id", tenantId.toString())
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }
}
