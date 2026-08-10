-- +goose Up
-- +goose StatementBegin

CREATE TABLE payments
(
    id                 UUID                     NOT NULL,
    booking_id         UUID                     NOT NULL,
    amount_minor       BIGINT                   NOT NULL,
    currency           VARCHAR(3)               NOT NULL,
    status             VARCHAR(30)              NOT NULL DEFAULT 'PENDING',
    idempotency_key    VARCHAR(255) UNIQUE      NOT NULL,
    payment_url        TEXT,
    failure_reason     TEXT,
    booking_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    paid_at            TIMESTAMP WITH TIME ZONE,


    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uk_payments_booking UNIQUE (booking_id),
    CONSTRAINT uk_payments_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_payments_amount_positive CHECK (amount_minor > 0)
);
-- +goose StatementEnd


-- +goose Down
DROP TABLE IF EXISTS payments;