package service

import (
	"time"

	"github.com/google/uuid"
)

type CreatePaymentCommand struct {
	BookingID        uuid.UUID
	AmountMinor      int64
	Currency         string
	BookingExpiresAt time.Time
	IdempotencyKey   string
}
