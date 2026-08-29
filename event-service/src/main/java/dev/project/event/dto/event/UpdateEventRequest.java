package dev.project.event.dto.event;

import dev.project.event.repository.entity.enums.EventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateEventRequest(
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt,
        @NotBlank String city,
        @NotBlank String venueName,
        String venueAddress
) {
}
