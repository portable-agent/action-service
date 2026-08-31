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
  private final long version;
  private final UUID tenantId;
  private final UUID actorId;
  private final String requestKey;
  private final String kind;
  private final String connector;
  private final Map<String, Object> payload;
  private final String payloadHash;
  private ActionStatus status;
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
        createdAt,
        updatedAt);
  }

  public void applyDecision(ActionDecision decision, String checkedHash, Instant now) {
    if (status != ActionStatus.AWAITING_APPROVAL) {
      throw new IllegalStateException("Action is not waiting for approval");
    }
    if (!payloadHash.equals(checkedHash)) {
      throw new IllegalArgumentException("Payload hash does not match");
    }
    status = decision == ActionDecision.CONFIRM ? ActionStatus.APPROVED : ActionStatus.CANCELLED;
    updatedAt = Objects.requireNonNull(now);
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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
