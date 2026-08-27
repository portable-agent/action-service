package dev.portableagent.action.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "action_dispatch_outbox")
public class ActionDispatchOutbox {
    @Id
    private UUID id;

    @Column(name = "action_id", nullable = false, updatable = false)
    private UUID actionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    protected ActionDispatchOutbox() {}

    public static ActionDispatchOutbox pending(UUID actionId, Instant now) {
        var outbox = new ActionDispatchOutbox();
        outbox.id = UUID.randomUUID();
        outbox.actionId = actionId;
        outbox.createdAt = now;
        return outbox;
    }

    public void markDispatched(Instant now) {
        dispatchedAt = now;
        attempts++;
        lastError = null;
    }

    public void markFailed(String error) {
        attempts++;
        lastError = error == null ? "Unknown dispatch error" : error.substring(0, Math.min(error.length(), 1000));
    }

    public UUID getActionId() { return actionId; }
}
