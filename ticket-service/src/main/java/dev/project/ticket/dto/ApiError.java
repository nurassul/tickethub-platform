package dev.project.ticket.dto;

public record ApiError(
        int status,
        String error,
        String message
) {
}