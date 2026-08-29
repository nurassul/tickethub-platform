package dev.project.booking.dto.feign;

import java.math.BigDecimal;
import java.util.UUID;

public record ValidatedSeatResponse(
        UUID seatId,
        BigDecimal price
) {
}
