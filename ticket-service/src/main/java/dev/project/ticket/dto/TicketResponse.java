package dev.project.ticket.dto;

import dev.project.ticket.repository.entity.enums.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        UUID bookingId,
        UUID eventId,
        UUID seatId,
        TicketStatus status,
        Instant createdAt
) {
}
