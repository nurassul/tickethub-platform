package domain

import "errors"

var (
	ErrPaymentNotFound       = errors.New("payment not found")
	ErrPaymentAlreadyExists  = errors.New("payment already exists")
	ErrInvalidPaymentStatus  = errors.New("invalid payment status")
	ErrInvalidAmount         = errors.New("invalid payment amount")
	ErrInvalidCurrency       = errors.New("invalid payment currency")
	ErrBookingExpired        = errors.New("booking expired")
	ErrIdempotencyConflict   = errors.New("idempotency conflict")
	ErrInvalidIdempotencyKey = errors.New("invalid idempotency key")
	ErrInvalidPaymentID      = errors.New("invalid payment id")
	ErrInvalidFailureReason  = errors.New("invalid failure reason")
)
