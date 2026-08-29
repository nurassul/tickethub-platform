package dev.project.event.dto.seat.booking;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ValidateSeatsRequest(
        @NotEmpty List<@NotNull UUID> seatIds
) {
}
