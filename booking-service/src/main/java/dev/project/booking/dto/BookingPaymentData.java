package dev.project.booking.dto;

import dev.project.booking.repository.entity.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingPaymentData(
        UUID bookingId,
        BookingStatus status,
        LocalDateTime expiresAt,
        BigDecimal totalPrice
) {
}
