package dev.portableagent.action.controller;

import dev.portableagent.action.api.ActionsApi;
import dev.portableagent.action.api.model.ActionDecisionRequest;
import dev.portableagent.action.api.model.ActionResponse;
import dev.portableagent.action.api.model.ProposeActionRequest;
import dev.portableagent.action.service.ActionService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActionController implements ActionsApi {
  private final ActionService actionService;

  public ActionController(ActionService actionService) {
    this.actionService = actionService;
  }

  @Override
  public ResponseEntity<ActionResponse> proposeAction(ProposeActionRequest request) {
    var jwt = jwt();
    var action = actionService.create(tenantId(jwt), userId(jwt), ActionMapper.toCommand(request));
    return ResponseEntity.created(URI.create("/api/v1/actions/" + action.getId()))
        .body(ActionMapper.toResponse(action));
  }

  @Override
  public ResponseEntity<ActionResponse> getAction(UUID actionId) {
    var action = actionService.get(tenantId(jwt()), actionId);
    return ResponseEntity.ok(ActionMapper.toResponse(action));
  }

  @Override
  public ResponseEntity<ActionResponse> decideAction(UUID actionId, ActionDecisionRequest request) {
    var action = actionService.decide(tenantId(jwt()), actionId, ActionMapper.toCommand(request));
    return ResponseEntity.accepted().body(ActionMapper.toResponse(action));
  }

  private Jwt jwt() {
    var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof Jwt jwt) {
      return jwt;
    }
    throw new IllegalStateException("JWT principal is missing");
  }

  private UUID tenantId(Jwt jwt) {
    return UUID.fromString(jwt.getClaimAsString("tenant_id"));
  }

  private UUID userId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }
}
