package repository

import (
	"context"
	"fmt"
	"payment-service/internal/domain"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

type OutboxRepository struct {
	pool *pgxpool.Pool
}

func NewOutboxRepository(pool *pgxpool.Pool) *OutboxRepository {
	return &OutboxRepository{
		pool: pool,
	}
}

func (r *OutboxRepository) FindUnpublished(
	ctx context.Context,
	limit int,
) ([]domain.OutboxEvent, error) {
	query := `
	SELECT id, topic, message_key, event_type, payload
	FROM outbox_events
	WHERE published_at IS NULL
	ORDER BY created_at ASC
	LIMIT $1
	`

	rows, err := r.pool.Query(ctx, query, limit)
	if err != nil {
		return nil, fmt.Errorf("find unpublished outbox events: %w", err)
	}
	defer rows.Close()

	events := make([]domain.OutboxEvent, 0)

	for rows.Next() {
		var event domain.OutboxEvent

		err := rows.Scan(
			&event.ID,
			&event.Topic,
			&event.MessageKey,
			&event.EventType,
			&event.Payload,
		)
		if err != nil {
			return nil, fmt.Errorf("scan outbox event: %w", err)
		}

		events = append(events, event)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate outbox events: %w", err)
	}

	return events, nil

}

func (r *OutboxRepository) MarkPublished(
	ctx context.Context,
	eventID uuid.UUID,
) error {
	query := `
	UPDATE outbox_events
	SET published_at = NOW(),
	    last_error = NULL
	WHERE id = $1
		AND published_at IS NULL
	`

	_, err := r.pool.Exec(ctx, query, eventID)
	if err != nil {
		return fmt.Errorf("mark outbox event published: %w", err)
	}

	return nil
}

func (r *OutboxRepository) MarkAttemptFailed(
	ctx context.Context,
	eventID uuid.UUID,
	cause error,
) error {
	query := `
	UPDATE outbox_events
	SET attempts = attempts + 1,
	    last_error = $2
	WHERE id = $1
		AND published_at IS NULL
	`

	_, err := r.pool.Exec(ctx, query, eventID, cause.Error())
	if err != nil {
		return fmt.Errorf("mark outbox publish attempt failed: %w", err)
	}

	return nil
}
