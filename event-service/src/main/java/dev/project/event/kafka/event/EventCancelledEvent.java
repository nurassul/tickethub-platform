package dev.project.event.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record EventCancelledEvent(
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
            UUID eventId,
            Instant cancelledAt
    ) {
    }
}

