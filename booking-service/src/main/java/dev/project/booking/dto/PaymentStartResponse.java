package dev.project.booking.dto;

import java.time.Instant;
import java.util.UUID;

public record PaymentStartResponse(
        UUID paymentId,
        UUID bookingId,
        String status,
        String paymentUrl,
        Instant expiresAt
) {

}
