package dev.project.event.dto.event;

import dev.project.event.repository.entity.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateEventRequest(
        String title,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String city,
        String venueName,
        String venueAddress
) {
}
