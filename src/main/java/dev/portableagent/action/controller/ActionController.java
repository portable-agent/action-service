package dev.portableagent.action.controller;

import dev.portableagent.action.dto.ActionResponse;
import dev.portableagent.action.dto.CreateActionRequest;
import dev.portableagent.action.dto.DecideActionRequest;
import dev.portableagent.action.service.ActionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/actions")
public class ActionController {
  private final ActionService actionService;

  public ActionController(ActionService actionService) {
    this.actionService = actionService;
  }

  @PostMapping
  public ResponseEntity<ActionResponse> create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateActionRequest request) {
    var action = actionService.create(tenantId(jwt), userId(jwt), request);
    return ResponseEntity.created(URI.create("/api/v1/actions/" + action.getId()))
        .body(ActionResponse.from(action));
  }

  @GetMapping("/{actionId}")
  public ActionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID actionId) {
    return ActionResponse.from(actionService.get(tenantId(jwt), actionId));
  }

  @PostMapping("/{actionId}/decisions")
  public ResponseEntity<ActionResponse> decide(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID actionId,
      @Valid @RequestBody DecideActionRequest request) {
    var action = actionService.decide(tenantId(jwt), actionId, request);
    return ResponseEntity.accepted().body(ActionResponse.from(action));
  }

  private UUID tenantId(Jwt jwt) {
    return UUID.fromString(jwt.getClaimAsString("tenant_id"));
  }

  private UUID userId(Jwt jwt) {
    return UUID.fromString(jwt.getSubject());
  }
}
