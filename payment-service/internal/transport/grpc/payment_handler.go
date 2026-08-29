package grpctransport

import (
	"context"
	paymentv1 "payment-service/gen/payment/v1"
	"payment-service/internal/domain"
	"payment-service/internal/service"

	"github.com/google/uuid"
)

type PaymentGRPCHandler struct {
	paymentv1.UnimplementedPaymentServiceServer
	paymentService PaymentService
}

func NewPaymentGRPCHandler(
	paymentService PaymentService,
) *PaymentGRPCHandler {
	return &PaymentGRPCHandler{
		paymentService: paymentService,
	}
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
}
