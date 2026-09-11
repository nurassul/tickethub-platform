package repository

import (
	"context"
	"fmt"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

type BookingExpiredEventRepository struct {
	pool *pgxpool.Pool
}

func NewBookingExpiredEventRepository(
	pool *pgxpool.Pool,
) *BookingExpiredEventRepository {
	return &BookingExpiredEventRepository{
		pool: pool,
	}
}

func (r *BookingExpiredEventRepository) Process(
	ctx context.Context,
	eventID uuid.UUID,
	bookingID uuid.UUID,
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

	queryUpdate := `
		UPDATE payments
		SET status = 'EXPIRED',
		    updated_at = NOW()
		WHERE booking_id = $1
			AND status = 'PENDING'
		`
	updateResult, err := tx.Exec(ctx, queryUpdate, bookingID)
	if err != nil {
		return false, fmt.Errorf("mark payment expired: %w", err)
	}

	paymentExpired := updateResult.RowsAffected() == 1

	if err := tx.Commit(ctx); err != nil {
		return false, fmt.Errorf("commit booking expired event: %w", err)
	}

	return paymentExpired, nil

}
