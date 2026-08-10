package repository

import (
	"errors"
	"payment-service/internal/domain"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

const (
	uniqueViolationCode = "23505"
	paymentColumns      = `
        id,
        booking_id,
        amount_minor,
        currency,
        status,
        idempotency_key,
        payment_url,
        failure_reason,
        booking_expires_at,
        created_at,
        updated_at,
        paid_at
    `
)

type PaymentRepository struct {
	pool *pgxpool.Pool
}

func NewPaymentRepository(
	pool *pgxpool.Pool,
) *PaymentRepository {
	return &PaymentRepository{
		pool: pool,
	}
}

type rowScanner interface {
	Scan(dest ...any) error
}

func scanPayment(
	row rowScanner,
) (*domain.Payment, error) {
	var payment domain.Payment

	err := row.Scan(
		&payment.ID,
		&payment.BookingID,
		&payment.AmountMinor,
		&payment.Currency,
		&payment.Status,
		&payment.IdempotencyKey,
		&payment.PaymentURL,
		&payment.FailureReason,
		&payment.BookingExpiresAt,
		&payment.CreatedAt,
		&payment.UpdatedAt,
		&payment.PaidAt,
	)

	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, domain.ErrPaymentNotFound
		}

		return nil, err
	}

	return &payment, nil
}
