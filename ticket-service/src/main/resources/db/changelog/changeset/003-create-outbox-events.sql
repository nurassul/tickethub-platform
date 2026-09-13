CREATE TABLE outbox_events
(
    id              UUID                     NOT NULL,
    topic           VARCHAR(255)              NOT NULL,
    message_key     VARCHAR(255)              NOT NULL,
    event_type      VARCHAR(100)              NOT NULL,
    payload         JSONB                    NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMP WITH TIME ZONE,
    attempts        INTEGER                  NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_error      TEXT,

    CONSTRAINT pk_outbox_events PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_events_unpublished
    ON outbox_events (next_attempt_at, created_at)
    WHERE published_at IS NULL;