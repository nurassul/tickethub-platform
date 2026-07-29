CREATE TABLE bookings
(
    id             UUID                        NOT NULL,
    event_id       UUID                        NOT NULL,
    customer_email VARCHAR(255),
    customer_phone VARCHAR(20),
    status         VARCHAR(30)                 NOT NULL DEFAULT 'HOLD',
    expires_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT pk_bookings PRIMARY KEY (id),

    CONSTRAINT chk_bookings_contact
        CHECK (
            customer_email IS NOT NULL
                OR customer_phone IS NOT NULL
            )
);

CREATE TABLE booking_seats
(
    id               UUID           NOT NULL,
    booking_id       UUID           NOT NULL,
    seat_id          UUID           NOT NULL,
    price_at_booking DECIMAL(12, 2) NOT NULL,

    CONSTRAINT pk_booking_seats PRIMARY KEY (id),

    CONSTRAINT uk_booking_seat
        UNIQUE (booking_id, seat_id),

    CONSTRAINT chk_booking_seat_price
        CHECK (price_at_booking > 0),

    CONSTRAINT fk_booking_seats_booking
        FOREIGN KEY (booking_id)
            REFERENCES bookings (id)
            ON DELETE CASCADE
);

CREATE TABLE seat_reservations
(
    id          UUID                        NOT NULL,
    booking_id  UUID                        NOT NULL,
    event_id    UUID                        NOT NULL,
    seat_id     UUID                        NOT NULL,
    status      VARCHAR(20)                 NOT NULL,
    expires_at  TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT pk_seat_reservations PRIMARY KEY (id),

    CONSTRAINT uk_active_event_seat
        UNIQUE (event_id, seat_id),

    CONSTRAINT fk_seat_reservations_booking
        FOREIGN KEY (booking_id)
            REFERENCES bookings (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_bookings_status_expires
    ON bookings (status, expires_at);

CREATE INDEX idx_booking_seats_booking
    ON booking_seats (booking_id);

CREATE INDEX idx_reservations_booking
    ON seat_reservations (booking_id);