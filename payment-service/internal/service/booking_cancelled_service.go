package service

import (
	"context"
	"time"

	"github.com/google/uuid"
)

type BookingCancelledService struct {
	processor BookingCancelledProcessor
}

func NewBookingCancelledService(
	processor BookingCancelledProcessor,
) *BookingCancelledService {
	return &BookingCancelledService{
		processor: processor,
	}
}

func (s *BookingCancelledService) HandleCancelled(
	ctx context.Context,
	eventID uuid.UUID,
	bookingID uuid.UUID,
	cancelledAt time.Time,
) error {
	return s.processor.ProcessCancelled(ctx, eventID, bookingID, cancelledAt)
}

type BookingCancelledProcessor interface {
	ProcessCancelled(
		ctx context.Context,
		eventID uuid.UUID,
		bookingID uuid.UUID,
		cancelledAt time.Time,
	) error
}
