package kafka

import (
	"time"

	"github.com/google/uuid"
)

const BookingPaymentRejectedType = "booking.payment-rejected"

type BookingPaymentRejected struct {
	EventID       uuid.UUID                     `json:"eventId"`
	EventType     string                        `json:"eventType"`
	EventVersion  int                           `json:"eventVersion"`
	OccurredAt    time.Time                     `json:"occurredAt"`
	Producer      string                        `json:"producer"`
	AggregateType string                        `json:"aggregateType"`
	AggregateID   uuid.UUID                     `json:"aggregateId"`
	CorrelationID uuid.UUID                     `json:"correlationId"`
	Payload       BookingPaymentRejectedPayload `json:"payload"`
}

type BookingPaymentRejectedPayload struct {
	BookingID uuid.UUID `json:"bookingId"`
	PaymentID uuid.UUID `json:"paymentId"`
	Reason    string    `json:"reason"`
}
