package service

import (
	"context"
	"fmt"
	"payment-service/internal/domain"
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

	payment, err := s.paymentRepository.MarkSucceeded(ctx, paymentID)
	if err != nil {
		return nil, fmt.Errorf("mark succeeded payment: %w", err)
	}

	return payment, nil
}
