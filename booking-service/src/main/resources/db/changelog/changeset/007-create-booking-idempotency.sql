CREATE TABLE booking_idempotency
(
    id               UUID                     NOT NULL PRIMARY KEY,
    guest_token_hash VARCHAR(64)              NOT NULL,
    idempotency_key  VARCHAR(255)             NOT NULL,
    request_hash     VARCHAR(64)              NOT NULL,
    booking_id       UUID                     NOT NULL REFERENCES bookings(id),
    response_status  INTEGER                  NOT NULL,
    response_body    TEXT                     NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_booking_idempotency_guest_key UNIQUE (guest_token_hash, idempotency_key)
);
