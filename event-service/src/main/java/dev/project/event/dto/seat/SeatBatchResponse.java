package dev.project.event.dto.seat;

import java.util.List;
import java.util.UUID;

public record SeatBatchResponse(
        UUID eventID,
        List<SeatResponse> seats
) {
}
