package dev.project.event.api.exceptions;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
        String messsage,
        String detailedMessage,
        LocalDateTime errorTime
) {
}
