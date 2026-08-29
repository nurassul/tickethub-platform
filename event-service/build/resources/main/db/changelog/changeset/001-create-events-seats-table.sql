CREATE TABLE events
(
    id            UUID                        NOT NULL,
    title         VARCHAR(200)                NOT NULL,
    description   TEXT,
    starts_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    ends_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    city          VARCHAR(255)                NOT NULL,
    venue_name    VARCHAR(255)                NOT NULL,
    venue_address VARCHAR(255),
    status        VARCHAR(30)                 NOT NULL DEFAULT 'DRAFT',
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT pk_events PRIMARY KEY (id),

    CONSTRAINT chk_events_dates
        CHECK (ends_at > starts_at)
);

CREATE TABLE seats
(
    id          UUID           NOT NULL,
    event_id    UUID           NOT NULL,
    sector      VARCHAR(50)    NOT NULL,
    row_number  VARCHAR(20)    NOT NULL,
    seat_number VARCHAR(20)    NOT NULL,
    type        VARCHAR(30)    NOT NULL,
    price       DECIMAL(12, 2) NOT NULL,

    CONSTRAINT pk_seats PRIMARY KEY (id),

    CONSTRAINT uk_event_seat_position
        UNIQUE (event_id, sector, row_number, seat_number),

    CONSTRAINT chk_seats_price_positive
        CHECK (price > 0),

    CONSTRAINT fk_seats_on_event
        FOREIGN KEY (event_id)
            REFERENCES events (id)
);