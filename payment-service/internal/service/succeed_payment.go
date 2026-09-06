package service

import (
	"context"
	"fmt"
	"payment-service/internal/domain"
	"payment-service/internal/kafka"
	"time"

	"github.com/google/uuid"
)

func (s *PaymentService) MarkSucceeded(
	ctx context.Context,
	paymentID uuid.UUID,
) (*domain.Payment, error) {
	now := time.Now().UTC()
	if paymentID == uuid.Nil {
		return nil, domain.ErrInvalidPaymentID
	}

	paymentCheck, err := s.GetByID(ctx, paymentID)
	if err != nil {
		return nil, fmt.Errorf("get payment by id: %w", err)
	}
	if paymentCheck.Status != domain.PaymentPending {
		if paymentCheck.Status == domain.PaymentSucceeded {
			return paymentCheck, nil
		}

		return nil, domain.ErrInvalidPaymentStatus
	}

	if !paymentCheck.BookingExpiresAt.After(now) {
		return nil, domain.ErrBookingExpired
	}

	paymentForEvent := *paymentCheck
	paymentForEvent.Status = domain.PaymentSucceeded
	paymentForEvent.PaidAt = &now

	outboxEvent, err := buildOutboxEvent(
		&paymentForEvent,
		kafka.PaymentSucceededEventType,
		s.paymentEventsTopic,
	)
	if err != nil {
		return nil, fmt.Errorf("build outbox event: %w", err)
	}

	payment, err := s.paymentRepository.MarkSucceededAndSaveOutbox(
		ctx,
		paymentID,
		now,
		outboxEvent,
	)
	if err != nil {
		return nil, fmt.Errorf("mark succeeded payment: %w", err)
	}

	return payment, nil
}
