package repository

import (
	"context"
	"payment-service/internal/domain"

	"github.com/google/uuid"
)

func (r *PaymentRepository) FindByBookingID(
	ctx context.Context,
	bookingID uuid.UUID,
) (*domain.Payment, error) {
	query := `
    SELECT ` + paymentColumns + `
    FROM payments
    WHERE booking_id = $1
	`

	return scanPayment(
		r.pool.QueryRow(ctx, query, bookingID),
	)
}
