package service

import (
	"payment-service/internal/domain"
	"payment-service/internal/kafka"
	"time"

	"github.com/google/uuid"
)

func buildPaymentEvent(
	payment *domain.Payment,
	eventType string,
) kafka.PaymentEvent {
	return kafka.PaymentEvent{
		EventID:       uuid.New(),
		EventType:     eventType,
		EventVersion:  1,
		OccurredAt:    time.Now().UTC(),
		Producer:      "payment-service",
		AggregateType: "payment",
		AggregateID:   payment.ID,
		CorrelationID: payment.BookingID,
		Payload: kafka.PaymentPayload{
			PaymentID:     payment.ID,
			BookingID:     payment.BookingID,
			AmountMinor:   payment.AmountMinor,
			Currency:      payment.Currency,
			Status:        string(payment.Status),
			PaidAt:        payment.PaidAt,
			FailureReason: payment.FailureReason,
		},
	}
}
