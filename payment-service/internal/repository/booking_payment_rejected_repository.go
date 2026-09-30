package repository

import (
	"context"
	"fmt"
	"payment-service/internal/domain"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

type BookingPaymentRejectedRepository struct {
	pool *pgxpool.Pool
}

func NewBookingPaymentRejectedRepository(
	pool *pgxpool.Pool,
) *BookingPaymentRejectedRepository {
	return &BookingPaymentRejectedRepository{
		pool: pool,
	}
}

func (r *BookingPaymentRejectedRepository) ProcessRejected(
	ctx context.Context,
	eventID uuid.UUID,
	bookingID uuid.UUID,
	paymentID uuid.UUID,
	outboxEvent domain.OutboxEvent,
) (bool, error) {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return false, fmt.Errorf("begin transaction: %w", err)
	}
	defer func() {
		_ = tx.Rollback(ctx)
	}()

	query := `
	INSERT INTO processed_events (consumer_name, event_id)
	VALUES ($1, $2)
	ON CONFLICT (consumer_name, event_id) DO NOTHING
	`
	res, err := tx.Exec(ctx, query, consumerName, eventID)
	if err != nil {
		return false, fmt.Errorf("exec query: %w", err)
	}

	if res.RowsAffected() == 0 {
		return false, nil
	}

	querySelect := `
    SELECT ` + paymentColumns + `
    FROM payments
    WHERE id = $1
	FOR UPDATE
	`
	payment, err := scanPayment(
		tx.QueryRow(ctx, querySelect, paymentID),
	)
	if err != nil {
		return false, fmt.Errorf("payment not found: %w", err)
	}

	if payment.BookingID != bookingID {
		return false, fmt.Errorf("bookingId must be equal")
	}

	switch payment.Status {
	case domain.PaymentRefunded:
		if err := tx.Commit(ctx); err != nil {
			return false, fmt.Errorf("commit booking payment rejected event: %w", err)
		}
		return false, nil
	case domain.PaymentSucceeded:
	default:
		return false, fmt.Errorf("invalid payment status")
	}

	queryUpdate := `
		UPDATE payments
		SET status = 'REFUNDED',
		    updated_at = NOW()
		WHERE id = $1
			AND status = 'SUCCEEDED'
		`
	updateResult, err := tx.Exec(ctx, queryUpdate, paymentID)
	if err != nil {
		return false, fmt.Errorf("mark payment refunded: %w", err)
	}

	if updateResult.RowsAffected() != 1 {
		return false, fmt.Errorf("expected exactly one payment to be refunded")
	}

	insertOutboxQuery := `
		INSERT INTO outbox_events (
		    id,
		    topic,
		    message_key,
		    event_type,
		    payload
		)
		VALUES ($1, $2, $3, $4, $5)
	`
	_, err = tx.Exec(
		ctx,
		insertOutboxQuery,
		outboxEvent.ID,
		outboxEvent.Topic,
		outboxEvent.MessageKey,
		outboxEvent.EventType,
		outboxEvent.Payload,
	)
	if err != nil {
		return false, fmt.Errorf("save outbox event: %w", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return false, fmt.Errorf("commit booking payment rejected event: %w", err)
	}

	return true, nil

}
