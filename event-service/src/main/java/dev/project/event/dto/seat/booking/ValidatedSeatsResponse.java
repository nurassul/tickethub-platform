package dev.project.event.dto.seat.booking;

import java.util.List;
import java.util.UUID;

public record ValidatedSeatsResponse(
        UUID eventId,
        List<ValidatedSeatResponse> seats
){
}
