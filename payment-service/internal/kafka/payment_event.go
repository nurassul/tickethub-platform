package kafka

import (
	"time"

	"github.com/google/uuid"
)

const (
	PaymentSucceededEventType = "payment.succeeded"
	PaymentFailedEventType    = "payment.failed"
)

type PaymentEvent struct {
	EventID       uuid.UUID      `json:"eventId"`
	EventType     string         `json:"eventType"`
	EventVersion  int            `json:"eventVersion"`
	OccurredAt    time.Time      `json:"occurredAt"`
	Producer      string         `json:"producer"`
	AggregateType string         `json:"aggregateType"`
	AggregateID   uuid.UUID      `json:"aggregateId"`
	CorrelationID uuid.UUID      `json:"correlationId"`
	Payload       PaymentPayload `json:"payload"`
}

type PaymentPayload struct {
	PaymentID     uuid.UUID  `json:"paymentId"`
	BookingID     uuid.UUID  `json:"bookingId"`
	AmountMinor   int64      `json:"amountMinor"`
	Currency      string     `json:"currency"`
	Status        string     `json:"status"`
	PaidAt        *time.Time `json:"paidAt,omitempty"`
	FailureReason *string    `json:"failureReason,omitempty"`
}
