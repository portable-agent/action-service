-- V1 was a pre-MVP skeleton and did not store payload. The original data cannot be restored from a hash.
DELETE FROM action_dispatch_outbox;
DELETE FROM action_proposals;

ALTER TABLE action_proposals
    ADD COLUMN payload JSONB NOT NULL;
