package service

import (
	"context"
	"fmt"
	"payment-service/internal/domain"
	"payment-service/internal/kafka"
	"time"

	"github.com/google/uuid"
)

type BookingCancelledService struct {
	processor          BookingCancelledProcessor
	paymentEventsTopic string
}

func NewBookingCancelledService(
	processor BookingCancelledProcessor,
	paymentEventsTopic string,
) *BookingCancelledService {
	return &BookingCancelledService{
		processor:          processor,
		paymentEventsTopic: paymentEventsTopic,
	}
}

func (s *BookingCancelledService) HandleCancelled(
	ctx context.Context,
	eventID uuid.UUID,
	bookingID uuid.UUID,
	cancelledAt time.Time,
) error {
	return s.processor.ProcessCancelled(ctx, eventID, bookingID, cancelledAt, s.buildRefundOutbox)
}

func (s *BookingCancelledService) buildRefundOutbox(payment *domain.Payment,
) (domain.OutboxEvent, error) {

	paymentForEvent := *payment
	paymentForEvent.Status = domain.PaymentRefunded

	response, err := buildOutboxEvent(&paymentForEvent, kafka.PaymentRefundedEventType, s.paymentEventsTopic)
	if err != nil {
		return domain.OutboxEvent{}, fmt.Errorf("build outbox event: %w", err)
	}

	return response, nil
}

type BookingCancelledProcessor interface {
	ProcessCancelled(
		ctx context.Context,
		eventID uuid.UUID,
		bookingID uuid.UUID,
		cancelledAt time.Time,
		buildRefund func(*domain.Payment) (domain.OutboxEvent, error),
	) error
}
