package kafka

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"strings"

	"github.com/google/uuid"
	"github.com/twmb/franz-go/pkg/kgo"
)

type BookingPaymentRejectedHandler struct {
	service PaymentRejectionService
}

type PaymentRejectionService interface {
	HandleRejected(
		ctx context.Context,
		eventID uuid.UUID,
		bookingID uuid.UUID,
		paymentID uuid.UUID,
	) (bool, error)
}

func NewBookingPaymentRejectedHandler(
	service PaymentRejectionService,
) *BookingPaymentRejectedHandler {
	return &BookingPaymentRejectedHandler{
		service: service,
	}
}

func (h *BookingPaymentRejectedHandler) HandleRejected(
	ctx context.Context,
	record *kgo.Record,
) error {
	var bookingPaymentRejected BookingPaymentRejected

	if err := json.Unmarshal(record.Value, &bookingPaymentRejected); err != nil {
		return fmt.Errorf("unmarshal booking.payment-rejected event: %w", err)
	}

	if err := validatePaymentRejected(bookingPaymentRejected); err != nil {
		return fmt.Errorf("validatePaymentRejected err: %w", err)
	}

	rejected, err := h.service.HandleRejected(
		ctx,
		bookingPaymentRejected.EventID,
		bookingPaymentRejected.Payload.BookingID,
		bookingPaymentRejected.Payload.PaymentID,
	)
	if err != nil {
		return fmt.Errorf("handle rejected: %w", err)
	}

	log.Printf(
		"booking.payment-rejected handled: eventId=%s, paymentId=%s, refunded=%t",
		bookingPaymentRejected.EventID,
		bookingPaymentRejected.Payload.PaymentID,
		rejected,
	)

	return nil
}

func validatePaymentRejected(event BookingPaymentRejected) error {
	if event.EventType != BookingPaymentRejectedType {
		return fmt.Errorf("event type must be booking.payment-rejected")
	}

	if event.EventVersion != 1 {
		return fmt.Errorf("event version not equal to 1")
	}

	if event.OccurredAt.IsZero() {
		return fmt.Errorf("occurredAt incorrect state")
	}

	if event.Producer != "booking-service" {
		return fmt.Errorf("incorrect producer")
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

	if event.Payload.PaymentID == uuid.Nil {
		return fmt.Errorf("paymentId is nil")
	}

	if strings.TrimSpace(event.Payload.Reason) == "" {
		return fmt.Errorf("payload.reason must be not empty")
	}

	return nil
}
