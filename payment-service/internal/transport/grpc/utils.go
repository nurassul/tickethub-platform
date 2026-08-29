package grpctransport

import (
	"context"
	"errors"
	"fmt"
	"log"
	paymentv1 "payment-service/gen/payment/v1"
	"payment-service/internal/domain"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/peer"
	"google.golang.org/grpc/status"
)

func convertError(err error) error {
	switch {
	case errors.Is(err, domain.ErrPaymentNotFound):
		return status.Error(codes.NotFound, err.Error())

	case errors.Is(err, domain.ErrInvalidPaymentID),
		errors.Is(err, domain.ErrInvalidAmount),
		errors.Is(err, domain.ErrInvalidCurrency),
		errors.Is(err, domain.ErrInvalidIdempotencyKey),
		errors.Is(err, domain.ErrInvalidFailureReason):

		return status.Error(codes.InvalidArgument, err.Error())

	case errors.Is(err, domain.ErrPaymentAlreadyExists),
		errors.Is(err, domain.ErrIdempotencyConflict):

		return status.Error(codes.AlreadyExists, err.Error())

	case errors.Is(err, domain.ErrBookingExpired),
		errors.Is(err, domain.ErrInvalidPaymentStatus):

		return status.Error(
			codes.FailedPrecondition,
			err.Error(),
		)

	default:
		log.Printf("unexpected gRPC error: %v", err)

		return status.Error(
			codes.Internal,
			"internal payment service error",
		)
	}
}

func toPaymentResponse(
	payment *domain.Payment,
) *paymentv1.PaymentResponse {
	response := &paymentv1.PaymentResponse{
		PaymentId:            payment.ID.String(),
		BookingId:            payment.BookingID.String(),
		AmountMinor:          payment.AmountMinor,
		Currency:             payment.Currency,
		Status:               string(payment.Status),
		BookingExpiresAtUnix: payment.BookingExpiresAt.Unix(),
		CreatedAtUnix:        payment.CreatedAt.Unix(),
		UpdatedAtUnix:        payment.UpdatedAt.Unix(),
	}
	if payment.PaymentURL != nil {
		response.PaymentUrl = *payment.PaymentURL
	}

	if payment.FailureReason != nil {
		response.FailureReason = *payment.FailureReason
	}
	if payment.PaidAt != nil {
		paidAtUnix := payment.PaidAt.Unix()
		response.PaidAtUnix = &paidAtUnix
	}

	return response
}

func GinStyleLogger() grpc.UnaryServerInterceptor {
	return func(ctx context.Context, req interface{}, info *grpc.UnaryServerInfo, handler grpc.UnaryHandler) (interface{}, error) {
		startTime := time.Now()

		resp, err := handler(ctx, req)

		duration := time.Since(startTime)

		clientIP := "unknown"
		if p, ok := peer.FromContext(ctx); ok {
			clientIP = p.Addr.String()
		}

		st, _ := status.FromError(err)
		statusCode := st.Code().String()

		timeStr := startTime.Format("2006/01/02 - 15:04:05")

		fmt.Printf("[GRPC] %s | %-14s | %12v | %15s | %s\n",
			timeStr,
			statusCode,
			duration,
			clientIP,
			info.FullMethod,
		)

		return resp, err
	}
}
