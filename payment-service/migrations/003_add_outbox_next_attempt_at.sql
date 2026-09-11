-- +goose Up

ALTER TABLE outbox_events
    ADD COLUMN next_attempt_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT NOW();

DROP INDEX IF EXISTS idx_outbox_events_unpublished;

CREATE INDEX idx_outbox_events_ready_to_publish
    ON outbox_events (next_attempt_at, created_at)
    WHERE published_at IS NULL;

-- +goose Down

DROP INDEX IF EXISTS idx_outbox_events_ready_to_publish;

ALTER TABLE outbox_events
DROP COLUMN next_attempt_at;

CREATE INDEX idx_outbox_events_unpublished
    ON outbox_events (created_at)
    WHERE published_at IS NULL;