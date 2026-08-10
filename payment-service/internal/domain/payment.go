package domain

import (
	"time"

	"github.com/google/uuid"
)

type PaymentStatus string

const (
	PaymentPending   PaymentStatus = "PENDING"
	PaymentSucceeded PaymentStatus = "SUCCEEDED"
	PaymentFailed    PaymentStatus = "FAILED"
	PaymentCancelled PaymentStatus = "CANCELLED"
	PaymentExpired   PaymentStatus = "EXPIRED"
	PaymentRefunded  PaymentStatus = "REFUNDED"
)

type Payment struct {
	ID               uuid.UUID
	BookingID        uuid.UUID
	AmountMinor      int64
	Currency         string
	Status           PaymentStatus
	IdempotencyKey   string
	PaymentURL       *string
	FailureReason    *string
	BookingExpiresAt time.Time
	CreatedAt        time.Time
	UpdatedAt        time.Time
	PaidAt           *time.Time
}
