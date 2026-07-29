package dev.project.event.dto.seat.booking;

import java.math.BigDecimal;
import java.util.UUID;

public record ValidatedSeatResponse(
        UUID seatId,
        BigDecimal price
) {
}
