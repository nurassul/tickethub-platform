CREATE TABLE tickets
(
    id         UUID                     NOT NULL,
    booking_id UUID                     NOT NULL,
    event_id   UUID                     NOT NULL,
    seat_id    UUID                     NOT NULL,
    status     VARCHAR(50)              NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),


    CONSTRAINT pk_tickets PRIMARY KEY (id),
    CONSTRAINT uk_tickets_booking_seat UNIQUE(booking_id, seat_id)
);

