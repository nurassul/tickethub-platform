package repository

import (
	"context"
	"fmt"
	"payment-service/internal/domain"

	"github.com/google/uuid"
)

func (r *PaymentRepository) MarkFailedAndSaveOutbox(
	ctx context.Context,
	paymentID uuid.UUID,
	reason string,
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
	SET status = 'FAILED',
		failure_reason = $2,
		updated_at = NOW()
	WHERE id = $1
	AND status = 'PENDING'
	RETURNING ` + paymentColumns

	payment, err := scanPayment(
		tx.QueryRow(ctx, query, paymentID, reason),
	)
	if err != nil {
		return nil, fmt.Errorf("mark payment failed: %w", err)
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
		return nil, fmt.Errorf("commit failed payment and outbox: %w", err)
	}

	return payment, nil
}

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
