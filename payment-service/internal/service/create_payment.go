package service

import (
	"context"
	"errors"
	"fmt"
	"payment-service/internal/domain"
	"strings"
	"time"

	"github.com/google/uuid"
)

func (s *PaymentService) Create(
	ctx context.Context,
	command CreatePaymentCommand,
) (*domain.Payment, error) {
	now := time.Now().UTC()

	if !command.BookingExpiresAt.After(now) {
		return nil, domain.ErrBookingExpired
	}

	if command.AmountMinor <= 0 {
		return nil, domain.ErrInvalidAmount
	}

	validCurrency := strings.ToUpper(strings.TrimSpace(command.Currency))
	if len(validCurrency) != 3 {
		return nil, domain.ErrInvalidCurrency
	}

	idempotencyKey := strings.TrimSpace(command.IdempotencyKey)
	if idempotencyKey == "" {
		return nil, domain.ErrInvalidIdempotencyKey
	}

	paymentByKey, err := s.paymentRepository.FindByIdempotencyKey(ctx, idempotencyKey)
	if err == nil {
		if !(paymentByKey.BookingID == command.BookingID && paymentByKey.Currency == validCurrency && paymentByKey.AmountMinor == command.AmountMinor) {
			return nil, domain.ErrIdempotencyConflict
		}

		return paymentByKey, nil

	} else if !errors.Is(err, domain.ErrPaymentNotFound) {
		return nil, fmt.Errorf("find by idempotency key: %w", err)
	}

	paymentByBookingID, err := s.paymentRepository.FindByBookingID(ctx, command.BookingID)
	if err == nil {
		if !(paymentByBookingID.Currency == validCurrency && paymentByBookingID.AmountMinor == command.AmountMinor) {
			return nil, domain.ErrIdempotencyConflict
		}

		return paymentByBookingID, nil
	} else if !errors.Is(err, domain.ErrPaymentNotFound) {
		return nil, fmt.Errorf("find payment by booking id: %w", err)
	}

	paymentID := uuid.New()
	validatedURL := strings.TrimRight(s.mockCheckoutURL, "/")
	url := validatedURL + "/" + paymentID.String()
	payment := domain.Payment{
		ID:               paymentID,
		BookingID:        command.BookingID,
		AmountMinor:      command.AmountMinor,
		Currency:         validCurrency,
		Status:           domain.PaymentPending,
		IdempotencyKey:   idempotencyKey,
		PaymentURL:       &url,
		FailureReason:    nil,
		BookingExpiresAt: command.BookingExpiresAt.UTC(),
		CreatedAt:        now,
		UpdatedAt:        now,
		PaidAt:           nil,
	}

	savedPayment, err := s.paymentRepository.Create(ctx, &payment)
	if err != nil {
		return nil, fmt.Errorf(
			"create payment: %w",
			err,
		)
	}

	return savedPayment, nil
}
