package dev.portableagent.action.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "action_proposals",
        uniqueConstraints = @UniqueConstraint(name = "uq_action_idempotency", columnNames = {"tenant_id", "idempotency_key"}),
        indexes = {
            @Index(name = "idx_action_tenant_created", columnList = "tenant_id, created_at"),
            @Index(name = "idx_action_status_updated", columnList = "status, updated_at")
        })
public class ActionProposal {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Version
    private Long version;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false, length = 80)
    private String kind;

    @Column(nullable = false, updatable = false, length = 80)
    private String connector;

    @Column(name = "payload_hash", nullable = false, updatable = false, length = 64)
    private String payloadHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ActionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ActionProposal() {}

    public static ActionProposal propose(
            UUID tenantId,
            UUID actorId,
            String idempotencyKey,
            String kind,
            String connector,
            String payloadHash,
            Instant now) {
        var action = new ActionProposal();
        action.id = UUID.randomUUID();
        action.tenantId = Objects.requireNonNull(tenantId);
        action.actorId = Objects.requireNonNull(actorId);
        action.idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        action.kind = requireText(kind, "kind");
        action.connector = requireText(connector, "connector");
        action.payloadHash = requireText(payloadHash, "payloadHash");
        action.status = ActionStatus.AWAITING_APPROVAL;
        action.createdAt = Objects.requireNonNull(now);
        action.updatedAt = now;
        return action;
    }

    public void decide(ActionDecision decision, String confirmedPayloadHash, Instant now) {
        if (status != ActionStatus.AWAITING_APPROVAL) {
            throw new IllegalStateException("Action is not awaiting approval");
        }
        if (!payloadHash.equals(confirmedPayloadHash)) {
            throw new IllegalArgumentException("Payload hash does not match the reviewed action");
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

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getActorId() { return actorId; }
    public String getKind() { return kind; }
    public String getConnector() { return connector; }
    public String getPayloadHash() { return payloadHash; }
    public ActionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
