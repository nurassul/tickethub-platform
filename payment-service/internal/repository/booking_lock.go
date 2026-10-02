package repository

import (
	"context"
	"fmt"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
)

func lockBooking(
	ctx context.Context,
	tx pgx.Tx,
	bookingID uuid.UUID,
) error {
	query := `
	SELECT pg_advisory_xact_lock(hashtextextended($1, 0))
	`
	_, err := tx.Exec(ctx, query, bookingID.String())
	if err != nil {
		return fmt.Errorf("lock booking: %w", err)
	}

	return nil
}
