package dev.project.booking.api.exceptions.dto;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
        String messsage,
        String detailedMessage,
        LocalDateTime errorTime
) {
}
