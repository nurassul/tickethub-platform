package kafka

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"payment-service/internal/repository"

	"github.com/google/uuid"
	"github.com/twmb/franz-go/pkg/kgo"
)

type BookingExpiredHandler struct {
	processor *repository.BookingExpiredEventRepository
}

func NewBookingExpiredHandler(
	processor *repository.BookingExpiredEventRepository,
) *BookingExpiredHandler {
	return &BookingExpiredHandler{
		processor: processor,
	}
}

func (h *BookingExpiredHandler) Handle(
	ctx context.Context,
	record *kgo.Record,
) error {
	var bookingExpiredEvent BookingExpiredEvent

	if err := json.Unmarshal(record.Value, &bookingExpiredEvent); err != nil {
		return fmt.Errorf("unmarshal booking.expired event: %w", err)
	}

	if err := validate(bookingExpiredEvent); err != nil {
		return fmt.Errorf("validate event: %w", err)
	}

	if bookingExpiredEvent.EventType != BookingExpiredEventType {
		return fmt.Errorf("incorrect event type")
	}

	paymentExpired, err := h.processor.Process(
		ctx,
		bookingExpiredEvent.EventID,
		bookingExpiredEvent.Payload.BookingID,
	)
	if err != nil {
		return fmt.Errorf("process booking.expired event: %w", err)
	}

	if paymentExpired {
		log.Printf(
			"payment expired by booking event: eventId=%s, bookingId=%s, expiredAt=%s",
			bookingExpiredEvent.EventID,
			bookingExpiredEvent.Payload.BookingID,
			bookingExpiredEvent.Payload.ExpiredAt,
		)
	} else {
		log.Printf("booking event ignored: duplicate or payment is not pending: "+
			"eventId=%s, bookingId=%s, expiredAt=%s",
			bookingExpiredEvent.EventID,
			bookingExpiredEvent.Payload.BookingID,
			bookingExpiredEvent.Payload.ExpiredAt,
		)
	}

	return nil
}

func validate(event BookingExpiredEvent) error {
	if event.EventVersion != 1 {
		return fmt.Errorf("event version not equal to 1")
	}

	if event.EventID == uuid.Nil {
		return fmt.Errorf("event id is nil")
	}

	if event.AggregateType != "booking" {
		return fmt.Errorf("aggregate type not equal to 'booking'")
	}

	if event.AggregateID != event.Payload.BookingID {
		return fmt.Errorf("aggregateId not equal to bookingId")
	}

	if event.CorrelationID != event.Payload.BookingID {
		return fmt.Errorf("correlationId not equal to bookingId")
	}

	if event.Payload.BookingID == uuid.Nil {
		return fmt.Errorf("bookingId is nil")
	}

	if event.Payload.ExpiredAt.IsZero() {
		return fmt.Errorf("expiredAt is zero")
	}

	return nil
}
