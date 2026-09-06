package service

import (
	"encoding/json"
	"fmt"
	"payment-service/internal/domain"
)

func buildOutboxEvent(
	payment *domain.Payment,
	eventType string,
	topic string,
) (domain.OutboxEvent, error) {
	event := buildPaymentEvent(payment, eventType)

	payload, err := json.Marshal(event)
	if err != nil {
		return domain.OutboxEvent{}, fmt.Errorf("marshal payment event: %w", err)
	}

	return domain.OutboxEvent{
		ID:         event.EventID,
		Topic:      topic,
		MessageKey: payment.BookingID.String(),
		EventType:  event.EventType,
		Payload:    payload,
	}, nil

}
