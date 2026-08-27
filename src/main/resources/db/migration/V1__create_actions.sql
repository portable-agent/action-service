CREATE TABLE action_proposals (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    tenant_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    kind VARCHAR(80) NOT NULL,
    connector VARCHAR(80) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_action_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_action_tenant_created ON action_proposals (tenant_id, created_at DESC, id DESC);
CREATE INDEX idx_action_status_updated ON action_proposals (status, updated_at);

CREATE TABLE action_dispatch_outbox (
    id UUID PRIMARY KEY,
    action_id UUID NOT NULL REFERENCES action_proposals(id),
    created_at TIMESTAMPTZ NOT NULL,
    dispatched_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);

CREATE INDEX idx_action_outbox_pending ON action_dispatch_outbox (created_at) WHERE dispatched_at IS NULL;
