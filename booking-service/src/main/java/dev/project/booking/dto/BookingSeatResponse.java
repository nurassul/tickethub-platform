package dev.project.booking.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingSeatResponse(
        UUID seatId,
        BigDecimal price
) {
}
