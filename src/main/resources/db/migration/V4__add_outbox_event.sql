ALTER TABLE action_dispatch_outbox
    ADD COLUMN event_type VARCHAR(16) NOT NULL DEFAULT 'START',
    ADD COLUMN decision VARCHAR(16),
    ADD COLUMN payload_hash CHAR(64),
    ADD COLUMN next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE action_dispatch_outbox
    ALTER COLUMN event_type DROP DEFAULT;

ALTER TABLE action_dispatch_outbox
    ADD CONSTRAINT ck_action_outbox_event CHECK (
        (event_type = 'START' AND decision IS NULL AND payload_hash IS NULL)
        OR
        (event_type = 'DECISION' AND decision IN ('CONFIRM', 'CANCEL') AND payload_hash IS NOT NULL)
    );

CREATE UNIQUE INDEX uq_action_outbox_event ON action_dispatch_outbox (action_id, event_type);

CREATE INDEX ix_action_outbox_pending
    ON action_dispatch_outbox (next_attempt_at, created_at)
    WHERE dispatched_at IS NULL;
