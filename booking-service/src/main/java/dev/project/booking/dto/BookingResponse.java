package dev.project.booking.dto;

import dev.project.booking.repository.entity.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        String customerEmail,
        String customerPhone,
        UUID eventId,
        List<BookingSeatResponse> seats,
        BookingStatus status,
        LocalDateTime expiresAt,
        BigDecimal totalPrice
) {
}
