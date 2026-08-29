package dev.project.booking.integration.kafka.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentEvent(
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
            UUID paymentId,
            UUID bookingId,
            long amountMinor,
            String currency,
            String status,
            Instant paidAt,
            String failureReason
    ) {
    }

}
