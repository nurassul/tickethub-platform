package repository

import (
	"context"
	"errors"
	"fmt"
	"payment-service/internal/domain"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

type BookingCancelledRepository struct {
	pool *pgxpool.Pool
}

func NewBookingCancelledRepository(
	pool *pgxpool.Pool,
) *BookingCancelledRepository {
	return &BookingCancelledRepository{
		pool: pool,
	}
}

func (r *BookingCancelledRepository) ProcessCancelled(
	ctx context.Context,
	eventID uuid.UUID,
	bookingID uuid.UUID,
	cancelledAt time.Time,
	buildRefund func(*domain.Payment) (domain.OutboxEvent, error),
) error {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("begin transaction: %w", err)
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
		return fmt.Errorf("exec query: %w", err)
	}

	if res.RowsAffected() == 0 {
		return nil
	}

	if err := lockBooking(ctx, tx, bookingID); err != nil {
		return err
	}

	queryCancelled := `
	INSERT INTO cancelled_bookings (booking_id, cancelled_at)
	VALUES ($1, $2)
	ON CONFLICT (booking_id) DO NOTHING
	`
	_, err = tx.Exec(ctx, queryCancelled, bookingID, cancelledAt)
	if err != nil {
		return fmt.Errorf("save booking cancellation: %w", err)
	}

	querySelect := `
    SELECT ` + paymentColumns + `
    FROM payments
    WHERE booking_id = $1
	FOR UPDATE
	`
	payment, err := scanPayment(
		tx.QueryRow(ctx, querySelect, bookingID),
	)

	if errors.Is(err, domain.ErrPaymentNotFound) {
		if err := tx.Commit(ctx); err != nil {
			return fmt.Errorf("commit booking payment cancelled event: %w", err)
		}
		return nil
	}
	if err != nil {
		return fmt.Errorf("payment not found: %w", err)
	}

	switch payment.Status {

	case domain.PaymentPending:
		queryUpdate := `
		UPDATE payments
		SET status = 'CANCELLED',
		    updated_at = NOW()
		WHERE id = $1
			AND status = 'PENDING'
		`
		_, err = tx.Exec(ctx, queryUpdate, payment.ID)
		if err != nil {
			return fmt.Errorf("mark payment cancelled: %w", err)
		}

	case domain.PaymentSucceeded:
		outboxEvent, err := buildRefund(payment)
		if err != nil {
			return err
		}

		queryUpdate := `
		UPDATE payments
		SET status = 'REFUNDED',
		    updated_at = NOW()
		WHERE id = $1
			AND status = 'SUCCEEDED'
		`
		res, err = tx.Exec(ctx, queryUpdate, payment.ID)
		if err != nil {
			return fmt.Errorf("mark payment refunded: %w", err)
		}
		if res.RowsAffected() != 1 {
			return fmt.Errorf("while updating payment, was updated: %d", res.RowsAffected())
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
			return fmt.Errorf("save outbox event: %w", err)
		}

	}

	if err := tx.Commit(ctx); err != nil {
		return fmt.Errorf("commit booking payment cancelled event: %w", err)
	}
	return nil
}
