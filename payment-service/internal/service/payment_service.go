package service

import (
	"context"
	"payment-service/internal/domain"
	"time"

	"github.com/google/uuid"
)

type PaymentService struct {
	paymentRepository  PaymentRepository
	mockCheckoutURL    string
	paymentEventsTopic string
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

	MarkSucceededAndSaveOutbox(
		ctx context.Context,
		paymentID uuid.UUID,
		paidAt time.Time,
		event domain.OutboxEvent,
	) (*domain.Payment, error)

	MarkFailedAndSaveOutbox(
		ctx context.Context,
		paymentID uuid.UUID,
		reason string,
		event domain.OutboxEvent,
	) (*domain.Payment, error)
}

func NewPaymentService(
	paymentRepository PaymentRepository,
	mockCheckoutURL string,
	paymentEventsTopic string,
) *PaymentService {
	return &PaymentService{
		paymentRepository:  paymentRepository,
		mockCheckoutURL:    mockCheckoutURL,
		paymentEventsTopic: paymentEventsTopic,
	}
}
