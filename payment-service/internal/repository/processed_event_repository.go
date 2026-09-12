package repository

import (
	"context"
	"fmt"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

const consumerName string = "payment-booking-events-v1"

type ProcessedEventRepository struct {
	pool *pgxpool.Pool
}

func NewProcessedEventRepository(
	pool *pgxpool.Pool,
) *ProcessedEventRepository {
	return &ProcessedEventRepository{
		pool: pool,
	}
}

func (r *ProcessedEventRepository) TryRegister(
	ctx context.Context,
	eventID uuid.UUID,
) (bool, error) {
	query := `
	INSERT INTO processed_events (consumer_name, event_id)
	VALUES ($1, $2)
	ON CONFLICT (consumer_name, event_id) DO NOTHING
	`

	res, err := r.pool.Exec(ctx, query, consumerName, eventID)
	if err != nil {
		return false, fmt.Errorf("try register processed event: %w", err)
	}

	if res.RowsAffected() == 1 {
		return true, nil
	}

	return false, nil
}
