package httptransport

import (
	"payment-service/internal/domain"
	"time"

	"github.com/google/uuid"
)

type PaymentDTOResponse struct {
	ID               uuid.UUID            `json:"id"`
	BookingID        uuid.UUID            `json:"booking_id"`
	AmountMinor      int64                `json:"amount_minor"`
	Currency         string               `json:"currency"`
	Status           domain.PaymentStatus `json:"status"`
	IdempotencyKey   string               `json:"idempotency_key"`
	PaymentURL       *string              `json:"payment_url"`
	FailureReason    *string              `json:"failure_reason"`
	BookingExpiresAt time.Time            `json:"booking_expires_at"`
	CreatedAt        time.Time            `json:"created_at"`
	UpdatedAt        time.Time            `json:"updated_at"`
	PaidAt           *time.Time           `json:"paid_at"`
}

type FailPaymentRequest struct {
	Reason string `json:"reason"`
}

func toPaymentResponse(payment *domain.Payment) PaymentDTOResponse {
	return PaymentDTOResponse{
		ID:               payment.ID,
		BookingID:        payment.BookingID,
		AmountMinor:      payment.AmountMinor,
		Currency:         payment.Currency,
		Status:           payment.Status,
		IdempotencyKey:   payment.IdempotencyKey,
		PaymentURL:       payment.PaymentURL,
		FailureReason:    payment.FailureReason,
		BookingExpiresAt: payment.BookingExpiresAt,
		CreatedAt:        payment.CreatedAt,
		UpdatedAt:        payment.UpdatedAt,
		PaidAt:           payment.PaidAt,
	}
}
