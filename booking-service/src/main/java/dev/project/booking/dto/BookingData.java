package dev.project.booking.dto;

import java.util.List;
import java.util.UUID;

public record BookingData(
        UUID bookingId,
        UUID eventId,
        List<UUID> seatIds
) {
}
