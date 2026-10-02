-- +goose Up
CREATE TABLE cancelled_bookings
(
    booking_id UUID PRIMARY KEY ,
    cancelled_at TIMESTAMP WITH TIME ZONE NOT NULL
);


-- +goose Down
DROP TABLE IF EXISTS cancelled_bookings;