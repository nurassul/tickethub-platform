package service

import (
	"context"
	"fmt"
	"payment-service/internal/domain"

	"github.com/google/uuid"
)

func (s *PaymentService) GetByID(
	ctx context.Context,
	paymentID uuid.UUID,
) (*domain.Payment, error) {
	if paymentID == uuid.Nil {
		return nil, domain.ErrInvalidPaymentID
	}

	payment, err := s.paymentRepository.FindByID(ctx, paymentID)
	if err != nil {
		return nil, fmt.Errorf("get by id: %w", err)
	}

	return payment, nil

}
