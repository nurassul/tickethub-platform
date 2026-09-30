package dev.project.booking.dto;

import dev.project.booking.dto.enums.PaymentConfirmationOutcome;

public record PaymentConfirmationResult(
        PaymentConfirmationOutcome outcome,
        BookingData bookingData,
        String rejectionReason
) {
}
