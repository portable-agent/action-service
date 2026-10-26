ALTER TABLE action_proposals DROP CONSTRAINT uq_action_idempotency;

ALTER TABLE action_proposals
    ADD CONSTRAINT uq_action_idempotency UNIQUE (tenant_id, actor_id, idempotency_key);
