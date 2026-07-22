package dev.project.event.dto.event;

import dev.project.event.repository.entity.enums.EventStatus;
import jakarta.persistence.Column;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String city,
        String venueName,
        String venueAddress,
        EventStatus status
) {
}
