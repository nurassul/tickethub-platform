package service

import (
	"context"
	"payment-service/internal/domain"

	"github.com/google/uuid"
)

type PaymentService struct {
	paymentRepository PaymentRepository
	mockCheckoutURL   string
}

type PaymentRepository interface {
	Create(
		ctx context.Context,
		payment *domain.Payment,
	) (*domain.Payment, error)

	FindByID(
		ctx context.Context,
		paymentID uuid.UUID,
	) (*domain.Payment, error)

	FindByBookingID(
		ctx context.Context,
		bookingID uuid.UUID,
	) (*domain.Payment, error)

	FindByIdempotencyKey(
		ctx context.Context,
		idempotencyKey string,
	) (*domain.Payment, error)

	MarkSucceeded(
		ctx context.Context,
		paymentID uuid.UUID,
	) (*domain.Payment, error)

	MarkFailed(
		ctx context.Context,
		paymentID uuid.UUID,
		reason string,
	) (*domain.Payment, error)
}

func NewPaymentService(
	paymentRepository PaymentRepository,
	mockCheckoutURL string,
) *PaymentService {
	return &PaymentService{
		paymentRepository: paymentRepository,
		mockCheckoutURL:   mockCheckoutURL,
	}
}
