package dev.project.ticket.integration.kafka.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketsGeneratedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        String aggregateType,
        UUID aggregateId,
        Payload payload
) {

    public record Payload(
            UUID bookingId,
            UUID eventId,
            List<UUID> ticketIds,
            String customerEmail
    ){}

}
