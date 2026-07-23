package dev.project.event.dto.seat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateSeatBatchRequest(
        @NotEmpty List<@Valid CreateSeatRequest> seats
) {
}
