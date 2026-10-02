package kafka

import (
	"time"

	"github.com/google/uuid"
)

const BookingCancelledType = "booking.cancelled"

type BookingCancelledEvent struct {
	EventID       uuid.UUID               `json:"eventId"`
	EventType     string                  `json:"eventType"`
	EventVersion  int                     `json:"eventVersion"`
	OccurredAt    time.Time               `json:"occurredAt"`
	Producer      string                  `json:"producer"`
	AggregateType string                  `json:"aggregateType"`
	AggregateID   uuid.UUID               `json:"aggregateId"`
	CorrelationID uuid.UUID               `json:"correlationId"`
	Payload       BookingCancelledPayload `json:"payload"`
}

type BookingCancelledPayload struct {
	BookingID   uuid.UUID `json:"bookingId"`
	CancelledAt time.Time `json:"cancelledAt"`
}
