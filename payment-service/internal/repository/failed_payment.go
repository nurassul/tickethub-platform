package repository

import (
	"context"
	"payment-service/internal/domain"

	"github.com/google/uuid"
)

func (r *PaymentRepository) MarkFailed(
	ctx context.Context,
	paymentID uuid.UUID,
	reason string,
) (*domain.Payment, error) {
	query := `
	UPDATE payments
	SET status = 'FAILED',
		failure_reason = $2,
		updated_at = NOW()
	WHERE id = $1
	AND status = 'PENDING'
	RETURNING ` + paymentColumns

	return scanPayment(r.pool.QueryRow(ctx, query, paymentID, reason))
}
