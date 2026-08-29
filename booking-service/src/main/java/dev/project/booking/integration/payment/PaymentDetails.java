package dev.project.booking.integration.payment;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record PaymentDetails(
        UUID paymentId,
        UUID bookingId,
        long amountMinor,
        String currency,
        String status,
        String paymentUrl,
        String failureReason,
        Instant bookingExpiresAt,
        Instant createdAt,
        Instant updatedAt,
        Instant paidAt
) {
}
