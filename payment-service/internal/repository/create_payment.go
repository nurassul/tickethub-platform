package repository

import (
	"context"
	"errors"
	"payment-service/internal/domain"

	"github.com/jackc/pgx/v5/pgconn"
)

func (r *PaymentRepository) Create(
	ctx context.Context,
	payment *domain.Payment,
) (*domain.Payment, error) {
	query := `
	INSERT INTO payments (id, booking_id, amount_minor, currency, status, idempotency_key, payment_url, failure_reason, booking_expires_at, created_at, updated_at, paid_at)
	VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
	RETURNING id, booking_id, amount_minor, currency, status, idempotency_key, payment_url, failure_reason, booking_expires_at, created_at, updated_at, paid_at
	`

	savedPayment, err := scanPayment(
		r.pool.QueryRow(
			ctx,
			query,
			payment.ID,
			payment.BookingID,
			payment.AmountMinor,
			payment.Currency,
			payment.Status,
			payment.IdempotencyKey,
			payment.PaymentURL,
			payment.FailureReason,
			payment.BookingExpiresAt,
			payment.CreatedAt,
			payment.UpdatedAt,
			payment.PaidAt,
		),
	)

	if err != nil {
		var pgErr *pgconn.PgError

		if errors.As(err, &pgErr) && pgErr.Code == uniqueViolationCode {
			return nil, domain.ErrPaymentAlreadyExists
		}

		return nil, err
	}

	return savedPayment, nil
}
