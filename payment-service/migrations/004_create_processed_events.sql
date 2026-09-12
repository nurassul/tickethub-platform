-- +goose Up
CREATE TABLE processed_events
(
    consumer_name VARCHAR(100)             NOT NULL,
    event_id      UUID                     NOT NULL,
    processed_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_processed_events PRIMARY KEY (consumer_name, event_id)
);


-- +goose Down
DROP TABLE IF EXISTS processed_events;