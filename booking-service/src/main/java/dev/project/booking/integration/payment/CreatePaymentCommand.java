package dev.project.booking.integration.payment;

import java.time.Instant;
import java.util.UUID;

public record CreatePaymentCommand(
        UUID bookingId,
        long amountMinor,
        String currency,
        Instant bookingExpiresAt,
        String idempotencyKey
) {
}
