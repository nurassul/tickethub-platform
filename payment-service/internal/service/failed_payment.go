package service

import (
	"context"
	"fmt"
	"payment-service/internal/domain"
	"strings"

	"github.com/google/uuid"
)

func (s *PaymentService) MarkFailed(
	ctx context.Context,
	paymentID uuid.UUID,
	reason string,
) (*domain.Payment, error) {
	if paymentID == uuid.Nil {
		return nil, domain.ErrInvalidPaymentID
	}

	reasonCorrect := strings.TrimSpace(reason)
	if reasonCorrect == "" || len(reasonCorrect) > 255 {
		return nil, domain.ErrInvalidFailureReason
	}

	paymentCheck, err := s.GetByID(ctx, paymentID)
	if err != nil {
		return nil, fmt.Errorf("get payment by id: %w", err)
	}
	if paymentCheck.Status != domain.PaymentPending {
		if paymentCheck.Status == domain.PaymentFailed {
			return paymentCheck, nil
		}

		return nil, domain.ErrInvalidPaymentStatus
	}

	payment, err := s.paymentRepository.MarkFailed(ctx, paymentID, reasonCorrect)
	if err != nil {
		return nil, fmt.Errorf("mark failed payment: %w", err)
	}

	return payment, nil
}
