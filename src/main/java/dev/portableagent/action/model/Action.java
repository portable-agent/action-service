package dev.portableagent.action.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Action {
  private final UUID id;
  private long version;
  private final UUID tenantId;
  private final UUID actorId;
  private final String requestKey;
  private final String kind;
  private final String connector;
  private final Map<String, Object> payload;
  private final String payloadHash;
  private ActionStatus status;
  private ActionResult result;
  private final Instant createdAt;
  private Instant updatedAt;

  private Action(
      UUID id,
      long version,
      UUID tenantId,
      UUID actorId,
      String requestKey,
      String kind,
      String connector,
      Map<String, Object> payload,
      String payloadHash,
      ActionStatus status,
      ActionResult result,
      Instant createdAt,
      Instant updatedAt) {
    this.id = Objects.requireNonNull(id);
    this.version = version;
    this.tenantId = Objects.requireNonNull(tenantId);
    this.actorId = Objects.requireNonNull(actorId);
    this.requestKey = requireText(requestKey, "requestKey");
    this.kind = requireText(kind, "kind");
    this.connector = requireText(connector, "connector");
    this.payload = copyPayload(payload);
    this.payloadHash = requireText(payloadHash, "payloadHash");
    this.status = Objects.requireNonNull(status);
    this.result = result;
    if (status == ActionStatus.SUCCEEDED && result == null) {
      throw new IllegalArgumentException("Succeeded action must have a result");
    }
    if (status != ActionStatus.SUCCEEDED && result != null) {
      throw new IllegalArgumentException("Only succeeded action can have a result");
    }
    this.createdAt = Objects.requireNonNull(createdAt);
    this.updatedAt = Objects.requireNonNull(updatedAt);
  }

  public static Action create(
      UUID tenantId,
      UUID actorId,
      String requestKey,
      String kind,
      String connector,
      Map<String, Object> payload,
      String payloadHash,
      Instant now) {
    return new Action(
        UUID.randomUUID(),
        0,
        tenantId,
        actorId,
        requestKey,
        kind,
        connector,
        payload,
        payloadHash,
        ActionStatus.AWAITING_APPROVAL,
        null,
        now,
        now);
  }

  public static Action fromData(
      UUID id,
      long version,
      UUID tenantId,
      UUID actorId,
      String requestKey,
      String kind,
      String connector,
      Map<String, Object> payload,
      String payloadHash,
      ActionStatus status,
      ActionResult result,
      Instant createdAt,
      Instant updatedAt) {
    return new Action(
        id,
        version,
        tenantId,
        actorId,
        requestKey,
        kind,
        connector,
        payload,
        payloadHash,
        status,
        result,
        createdAt,
        updatedAt);
  }

  public boolean applyDecision(ActionDecision decision, String checkedHash, Instant now) {
    Objects.requireNonNull(decision);
    Objects.requireNonNull(now);
    if (!payloadHash.equals(checkedHash)) {
      throw new IllegalArgumentException("Payload hash does not match");
    }
    var nextStatus =
        decision == ActionDecision.CONFIRM ? ActionStatus.APPROVED : ActionStatus.CANCELLED;
    if (decisionAlreadyApplied(decision)) {
      return false;
    }
    if (status != ActionStatus.AWAITING_APPROVAL) {
      throw new IllegalStateException("Action is not waiting for approval");
    }
    status = nextStatus;
    updatedAt = now;
    return true;
  }

  private boolean decisionAlreadyApplied(ActionDecision decision) {
    if (decision == ActionDecision.CANCEL) {
      return status == ActionStatus.CANCELLED;
    }
    return status == ActionStatus.APPROVED
        || status == ActionStatus.EXECUTING
        || status == ActionStatus.SUCCEEDED
        || status == ActionStatus.FAILED;
  }

  public boolean startExecution(Instant now) {
    Objects.requireNonNull(now);
    if (status == ActionStatus.EXECUTING) {
      return false;
    }
    if (status != ActionStatus.APPROVED) {
      throw new IllegalStateException("Action is not approved");
    }
    status = ActionStatus.EXECUTING;
    updatedAt = now;
    return true;
  }

  public boolean succeed(String eventId, Instant now) {
    var newResult = new ActionResult(eventId);
    Objects.requireNonNull(now);
    if (status == ActionStatus.SUCCEEDED) {
      if (newResult.equals(result)) {
        return false;
      }
      throw new IllegalStateException("Action already has a different result");
    }
    if (status != ActionStatus.EXECUTING) {
      throw new IllegalStateException("Action is not executing");
    }
    result = newResult;
    status = ActionStatus.SUCCEEDED;
    updatedAt = now;
    return true;
  }

  public boolean fail(Instant now) {
    Objects.requireNonNull(now);
    if (status == ActionStatus.FAILED) {
      return false;
    }
    if (status != ActionStatus.EXECUTING) {
      throw new IllegalStateException("Action is not executing");
    }
    status = ActionStatus.FAILED;
    updatedAt = now;
    return true;
  }

  public void markSaved() {
    version++;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }

  private static Map<String, Object> copyPayload(Map<String, Object> payload) {
    if (payload == null || payload.isEmpty()) {
      throw new IllegalArgumentException("payload must not be empty");
    }
    var copy = new LinkedHashMap<String, Object>();
    payload.forEach((key, value) -> copy.put(requireText(key, "payload key"), copyValue(value)));
    return Collections.unmodifiableMap(copy);
  }

  private static Object copyValue(Object value) {
    if (value instanceof Map<?, ?> map) {
      var copy = new LinkedHashMap<String, Object>();
      map.forEach(
          (key, child) -> {
            if (!(key instanceof String textKey)) {
              throw new IllegalArgumentException("payload key must be text");
            }
            copy.put(requireText(textKey, "payload key"), copyValue(child));
          });
      return Collections.unmodifiableMap(copy);
    }
    if (value instanceof List<?> list) {
      return list.stream().map(Action::copyValue).toList();
    }
    return value;
  }

  public UUID getId() {
    return id;
  }

  public long getVersion() {
    return version;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getActorId() {
    return actorId;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public String getKind() {
    return kind;
  }

  public String getConnector() {
    return connector;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  public String getPayloadHash() {
    return payloadHash;
  }

  public ActionStatus getStatus() {
    return status;
  }

  public ActionResult getResult() {
    return result;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
