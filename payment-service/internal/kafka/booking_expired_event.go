package kafka

import (
	"time"

	"github.com/google/uuid"
)

const BookingExpiredEventType = "booking.expired"

type BookingExpiredEvent struct {
	EventID       uuid.UUID                  `json:"eventId"`
	EventType     string                     `json:"eventType"`
	EventVersion  int                        `json:"eventVersion"`
	OccurredAt    time.Time                  `json:"occurredAt"`
	Producer      string                     `json:"producer"`
	AggregateType string                     `json:"aggregateType"`
	AggregateID   uuid.UUID                  `json:"aggregateId"`
	CorrelationID uuid.UUID                  `json:"correlationId"`
	Payload       BookingExpiredEventPayload `json:"payload"`
}

type BookingExpiredEventPayload struct {
	BookingID uuid.UUID `json:"bookingId"`
	ExpiredAt time.Time `json:"expiredAt"`
}
