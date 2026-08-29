package repository

import (
	"context"
	"payment-service/internal/domain"

	"github.com/google/uuid"
)

func (r *PaymentRepository) MarkSucceeded(
	ctx context.Context,
	paymentID uuid.UUID,
) (*domain.Payment, error) {
	query := `
	UPDATE payments
	SET status = 'SUCCEEDED',
	    paid_at = NOW(),
	    updated_at = NOW()
	WHERE id = $1
	AND status = 'PENDING'
	RETURNING id, booking_id, amount_minor, currency, status, idempotency_key, payment_url, failure_reason, booking_expires_at, created_at, updated_at, paid_at
	`

	return scanPayment(r.pool.QueryRow(ctx, query, paymentID))
}
