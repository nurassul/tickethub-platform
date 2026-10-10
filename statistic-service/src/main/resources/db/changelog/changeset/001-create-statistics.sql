--liquibase formatted sql

--changeset paind:001-create-statistics
CREATE TABLE statistics
(
    id             SMALLINT PRIMARY KEY CHECK (id = 1),
    total_bookings BIGINT NOT NULL DEFAULT 0 CHECK ( total_bookings >= 0 ),
    total_tickets  BIGINT NOT NULL DEFAULT 0 CHECK ( total_tickets >= 0 )
);

INSERT INTO statistics (id, total_bookings, total_tickets)
VALUES (1, 0, 0);

CREATE TABLE processed_events
(
    consumer_name VARCHAR(100) NOT NULL,
    event_id      UUID         NOT NULL,
    processed_at  timestamptz    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (consumer_name, event_id)
);