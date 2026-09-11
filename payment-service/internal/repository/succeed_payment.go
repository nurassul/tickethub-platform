package repository

import (
	"context"
	"fmt"
	"payment-service/internal/domain"
	"time"

	"github.com/google/uuid"
)

func (r *PaymentRepository) MarkSucceededAndSaveOutbox(
	ctx context.Context,
	paymentID uuid.UUID,
	paidAt time.Time,
	event domain.OutboxEvent,
) (*domain.Payment, error) {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return nil, fmt.Errorf("begin transaction: %w", err)
	}
	defer func() {
		_ = tx.Rollback(ctx)
	}()

	query := `
	UPDATE payments
	SET status = 'SUCCEEDED',
	    paid_at = $2,
	    updated_At = $2
	WHERE id = $1
		AND status = 'PENDING'
	RETURNING ` + paymentColumns

	payment, err := scanPayment(
		tx.QueryRow(ctx, query, paymentID, paidAt),
	)
	if err != nil {
		return nil, fmt.Errorf("mark payment succeeded: %w", err)
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
		event.ID,
		event.Topic,
		event.MessageKey,
		event.EventType,
		event.Payload,
	)
	if err != nil {
		return nil, fmt.Errorf("save outbox event: %w", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return nil, fmt.Errorf("commit succeeded payment and outbox: %w", err)
	}

	return payment, nil
}

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
