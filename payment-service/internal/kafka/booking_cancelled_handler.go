package kafka

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"time"

	"github.com/google/uuid"
	"github.com/twmb/franz-go/pkg/kgo"
)

type BookingCancelledHandler struct {
	service BookingCancellationService
}

func NewBookingCancelledHandler(
	service BookingCancellationService,
) *BookingCancelledHandler {
	return &BookingCancelledHandler{
		service: service,
	}
}

type BookingCancellationService interface {
	HandleCancelled(
		ctx context.Context,
		eventID uuid.UUID,
		bookingID uuid.UUID,
		cancelledAt time.Time,
	) error
}

func (h *BookingCancelledHandler) HandleCancelled(
	ctx context.Context,
	record *kgo.Record,
) error {
	var bookingCancelledEvent BookingCancelledEvent

	if err := json.Unmarshal(record.Value, &bookingCancelledEvent); err != nil {
		return fmt.Errorf("unmarshal booking.cancelled event: %w", err)
	}

	if err := validateCancelledEvent(bookingCancelledEvent); err != nil {
		return fmt.Errorf("validateCancelledEvent err: %w", err)
	}

	err := h.service.HandleCancelled(
		ctx,
		bookingCancelledEvent.EventID,
		bookingCancelledEvent.Payload.BookingID,
		bookingCancelledEvent.Payload.CancelledAt,
	)
	if err != nil {
		return fmt.Errorf("handle cancelled: %w", err)
	}

	log.Printf(
		"booking.cancelled handled: eventId=%s, bookingId=%s, cancelledAt=%v",
		bookingCancelledEvent.EventID,
		bookingCancelledEvent.Payload.BookingID,
		bookingCancelledEvent.Payload.CancelledAt,
	)

	return nil
}

func validateCancelledEvent(event BookingCancelledEvent) error {
	if event.EventVersion != 1 {
		return fmt.Errorf("event version not equal to 1")
	}

	if event.EventID == uuid.Nil {
		return fmt.Errorf("event id is nil")
	}

	if event.EventType != BookingCancelledType {
		return fmt.Errorf("event type is not valid")
	}

	if event.Producer != "booking-service" {
		return fmt.Errorf("event producer is not valid")
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

	if event.OccurredAt.IsZero() {
		return fmt.Errorf("occurredAt is zero")
	}

	if event.Payload.CancelledAt.IsZero() {
		return fmt.Errorf("cancelledAt is zero")
	}

	return nil
}
