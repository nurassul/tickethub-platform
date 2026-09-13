package dev.project.booking.integration.kafka.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingConfirmedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        String aggregateType,
        UUID aggregateId,
        UUID correlationId,
        Payload payload
) {
    public record Payload(
            UUID bookingId,
            UUID eventId,
            List<UUID> seatIds
    ){}
}

