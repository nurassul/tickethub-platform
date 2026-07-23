package dev.project.event.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt,
        @NotBlank String city,
        @NotBlank String venueName,
        String venueAddress
) {
}
