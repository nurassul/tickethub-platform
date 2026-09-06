package outbox

import (
	"context"
	"fmt"
	"log"
	"payment-service/internal/domain"

	"github.com/google/uuid"
)

type Repository interface {
	FindUnpublished(ctx context.Context, limit int) ([]domain.OutboxEvent, error)
	MarkPublished(ctx context.Context, eventID uuid.UUID) error
	MarkAttemptFailed(ctx context.Context, eventID uuid.UUID, cause error) error
}

type Publisher interface {
	PublishRaw(
		ctx context.Context,
		topic string,
		messageKey string,
		payload []byte,
	) error
}

type Relay struct {
	repository Repository
	publisher  Publisher
	batchSize  int
}

func NewRelay(
	repository Repository,
	publisher Publisher,
	batchSize int,
) *Relay {
	return &Relay{
		repository: repository,
		publisher:  publisher,
		batchSize:  batchSize,
	}
}

func (r *Relay) RunOnce(ctx context.Context) error {
	events, err := r.repository.FindUnpublished(ctx, r.batchSize)
	if err != nil {
		return fmt.Errorf("find unpublished outbox events: %w", err)
	}

	for _, event := range events {
		err := r.publisher.PublishRaw(
			ctx,
			event.Topic,
			event.MessageKey,
			event.Payload,
		)
		if err != nil {
			log.Printf(
				"kafka publish failed: eventId=%s, type=%s, topic=%s, error=%v",
				event.ID,
				event.EventType,
				event.Topic,
				err,
			)

			if markErr := r.repository.MarkAttemptFailed(ctx, event.ID, err); markErr != nil {
				return fmt.Errorf("mark outbox event published: %w", err)
			}
			return nil
		}

		if err := r.repository.MarkPublished(ctx, event.ID); err != nil {
			return fmt.Errorf("mark outbox event published: %w", err)
		}

		log.Printf(
			"kafka event published: eventId=%s, type=%s, topic=%s, key=%s",
			event.ID,
			event.EventType,
			event.Topic,
			event.MessageKey,
		)
	}

	return nil

}
