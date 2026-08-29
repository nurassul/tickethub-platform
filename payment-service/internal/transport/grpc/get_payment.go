package grpctransport

import (
	"context"
	paymentv1 "payment-service/gen/payment/v1"

	"github.com/google/uuid"
)

func (h *PaymentGRPCHandler) GetPayment(
	ctx context.Context,
	in *paymentv1.GetPaymentRequest,
) (*paymentv1.PaymentResponse, error) {
	paymentId, err := uuid.Parse(in.GetPaymentId())
	if err != nil {
		return nil, convertError(err)
	}

	payment, err := h.paymentService.GetByID(ctx, paymentId)
	if err != nil {
		return nil, convertError(err)
	}

	return toPaymentResponse(payment), nil
}
