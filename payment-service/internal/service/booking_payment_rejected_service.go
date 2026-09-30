package service

import (
	"context"
	"fmt"
	"payment-service/internal/domain"
	"payment-service/internal/kafka"

	"github.com/google/uuid"
)

type BookingPaymentRejectedService struct {
	paymentRepository         PaymentRepository
	paymentRejectionProcessor PaymentRejectionProcessor
	paymentEventsTopic        string
}

type PaymentRejectionProcessor interface {
	ProcessRejected(
		ctx context.Context,
		eventID uuid.UUID,
		bookingID uuid.UUID,
		paymentID uuid.UUID,
		outboxEvent domain.OutboxEvent,
	) (bool, error)
}

func NewBookingPaymentRejectedService(
	paymentRepository PaymentRepository,
	paymentRejectionProcessor PaymentRejectionProcessor,
	paymentEventsTopic string,
) *BookingPaymentRejectedService {
	return &BookingPaymentRejectedService{
		paymentRepository:         paymentRepository,
		paymentRejectionProcessor: paymentRejectionProcessor,
		paymentEventsTopic:        paymentEventsTopic,
	}
}

func (s *BookingPaymentRejectedService) HandleRejected(
	ctx context.Context,
	eventID uuid.UUID,
	bookingID uuid.UUID,
	paymentID uuid.UUID,
) (bool, error) {
	payment, err := s.paymentRepository.FindByID(ctx, paymentID)
	if err != nil {
		return false, fmt.Errorf("load payment: %w", err)
	}

	if payment.BookingID != bookingID {
		return false, fmt.Errorf("payment bookingID not equal")
	}

	paymentForEvent := *payment
	paymentForEvent.Status = domain.PaymentRefunded

	outboxEvent, err := buildOutboxEvent(
		&paymentForEvent,
		kafka.PaymentRefundedEventType,
		s.paymentEventsTopic,
	)
	if err != nil {
		return false, fmt.Errorf("outbox building: %w", err)
	}

	return s.paymentRejectionProcessor.ProcessRejected(
		ctx,
		eventID,
		bookingID,
		paymentID,
		outboxEvent,
	)
}
