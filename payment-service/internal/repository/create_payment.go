package repository

import (
	"context"
	"errors"
	"fmt"
	"payment-service/internal/domain"

	"github.com/jackc/pgx/v5/pgconn"
)

func (r *PaymentRepository) Create(
	ctx context.Context,
	payment *domain.Payment,
) (*domain.Payment, error) {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return nil, fmt.Errorf("begin transaction: %w", err)
	}
	defer func() {
		_ = tx.Rollback(ctx)
	}()

	if err := lockBooking(ctx, tx, payment.BookingID); err != nil {
		return nil, err
	}

	var cancelled bool

	queryCheck := `
	SELECT EXISTS(
		SELECT 1 FROM cancelled_bookings 
		         WHERE booking_id=$1
	)
	`
	err = tx.QueryRow(ctx, queryCheck, payment.BookingID).Scan(&cancelled)
	if err != nil {
		return nil, fmt.Errorf("check booking cancellation: %w", err)
	}
	if cancelled {
		return nil, domain.ErrBookingCancelled
	}

	query := `
	INSERT INTO payments (id, booking_id, amount_minor, currency, status, idempotency_key, payment_url, failure_reason, booking_expires_at, created_at, updated_at, paid_at)
	VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
	RETURNING id, booking_id, amount_minor, currency, status, idempotency_key, payment_url, failure_reason, booking_expires_at, created_at, updated_at, paid_at
	`

	savedPayment, err := scanPayment(
		tx.QueryRow(
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

	if err := tx.Commit(ctx); err != nil {
		return nil, fmt.Errorf("commit create payment: %w", err)
	}

	return savedPayment, nil
}
