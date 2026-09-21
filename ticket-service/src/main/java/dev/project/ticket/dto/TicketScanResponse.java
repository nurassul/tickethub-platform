package dev.project.ticket.dto;

public record TicketScanResponse(
        boolean isValid,
        String message
) {
}
