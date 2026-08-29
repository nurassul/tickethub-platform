package dev.project.event.dto.seat;

import dev.project.event.repository.entity.enums.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateSeatRequest(
        @NotBlank String sector,
        @NotBlank String rowNumber,
        @NotBlank String seatNumber,
        @NotNull SeatType type,
        @NotNull @Positive BigDecimal price
) {
}
