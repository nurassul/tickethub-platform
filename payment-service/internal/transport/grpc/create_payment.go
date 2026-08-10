package grpctransport

import (
	"context"
	"log"
	paymentv1 "payment-service/gen/payment/v1"
	"payment-service/internal/service"
	"time"

	"github.com/google/uuid"
)

func (h *PaymentGRPCHandler) CreatePayment(
	ctx context.Context,
	in *paymentv1.CreatePaymentRequest,
) (*paymentv1.PaymentResponse, error) {
	bookingId, err := uuid.Parse(in.GetBookingId())
	if err != nil {
		return nil, convertError(err)
	}

	bookingExpires := time.Unix(in.GetBookingExpiresAtUnix(), 0).UTC()
	paymentCommand := service.CreatePaymentCommand{
		BookingID:        bookingId,
		AmountMinor:      in.GetAmountMinor(),
		Currency:         in.GetCurrency(),
		BookingExpiresAt: bookingExpires,
		IdempotencyKey:   in.GetIdempotencyKey(),
	}

	payment, err := h.paymentService.Create(ctx, paymentCommand)
	if err != nil {
		log.Printf("failed to create payment: %v", err)
		return nil, convertError(err)
	}

	return toPaymentResponse(payment), nil
}
