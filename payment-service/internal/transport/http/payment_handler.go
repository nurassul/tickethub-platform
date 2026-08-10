package httptransport

import (
	"context"
	"payment-service/internal/domain"
	"payment-service/internal/service"

	"github.com/google/uuid"
)

type PaymentHTTPHandler struct {
	paymentService PaymentService
}

type PaymentService interface {
	GetByID(
		ctx context.Context,
		paymentID uuid.UUID,
	) (*domain.Payment, error)

	Create(
		ctx context.Context,
		command service.CreatePaymentCommand,
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

func NewPaymentHTTPHandler(
	paymentService PaymentService,
) *PaymentHTTPHandler {
	return &PaymentHTTPHandler{
		paymentService: paymentService,
	}
}
