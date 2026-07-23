package dev.project.event.dto.seat;

import dev.project.event.repository.entity.enums.SeatType;

import java.math.BigDecimal;
import java.util.UUID;

public record SeatResponse(
        UUID id,
        String sector,
        String rowNumber,
        String seatNumber,
        SeatType type,
        BigDecimal price
) {
}
