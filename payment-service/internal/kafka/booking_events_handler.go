package kafka

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/twmb/franz-go/pkg/kgo"
)

type BookingEventsHandler struct {
	bookingExpiredHandler         *BookingExpiredHandler
	bookingPaymentRejectedHandler *BookingPaymentRejectedHandler
	bookingCancelledHandler       *BookingCancelledHandler
}

func NewBookingEventsHandler(
	bookingExpiredHandler *BookingExpiredHandler,
	bookingPaymentRejectedHandler *BookingPaymentRejectedHandler,
	bookingCancelledHandler *BookingCancelledHandler,
) *BookingEventsHandler {
	return &BookingEventsHandler{
		bookingExpiredHandler:         bookingExpiredHandler,
		bookingPaymentRejectedHandler: bookingPaymentRejectedHandler,
		bookingCancelledHandler:       bookingCancelledHandler,
	}
}

func (h *BookingEventsHandler) Handle(
	ctx context.Context,
	record *kgo.Record,
) error {
	var header bookingEventHeader

	if err := json.Unmarshal(record.Value, &header); err != nil {
		return fmt.Errorf("unmarshal booking event header: %w", err)
	}

	validatedHeader := strings.TrimSpace(header.EventType)
	if validatedHeader == "" {
		return fmt.Errorf("event type is empty")
	}

	switch header.EventType {
	case BookingExpiredEventType:
		return h.bookingExpiredHandler.HandleExpired(ctx, record)
	case BookingPaymentRejectedType:
		return h.bookingPaymentRejectedHandler.HandleRejected(ctx, record)
	case "booking.confirmed":
		return nil
	case BookingCancelledType:
		return h.bookingCancelledHandler.HandleCancelled(ctx, record)
	default:
		return fmt.Errorf("invalid eventType: %q", header.EventType)
	}
}

type bookingEventHeader struct {
	EventType string `json:"eventType"`
}
