package dev.project.booking.dto.feign;

import java.util.List;
import java.util.UUID;

public record ValidateSeatsRequest(
        List<UUID> seatIds
) {
}