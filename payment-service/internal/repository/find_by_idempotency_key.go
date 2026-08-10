package repository

import (
	"context"
	"payment-service/internal/domain"
)

func (r *PaymentRepository) FindByIdempotencyKey(
	ctx context.Context,
	idempotencyKey string,
) (*domain.Payment, error) {
	query := `
    SELECT ` + paymentColumns + `
    FROM payments
    WHERE idempotency_key = $1
	`

	return scanPayment(
		r.pool.QueryRow(ctx, query, idempotencyKey),
	)

}
