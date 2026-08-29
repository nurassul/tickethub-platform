package repository

import (
	"context"
	"payment-service/internal/domain"

	"github.com/google/uuid"
)

func (r *PaymentRepository) FindByID(
	ctx context.Context,
	paymentID uuid.UUID,
) (*domain.Payment, error) {
	query := `
    SELECT ` + paymentColumns + `
    FROM payments
    WHERE id = $1
	`
	return scanPayment(
		r.pool.QueryRow(ctx, query, paymentID),
	)
}
