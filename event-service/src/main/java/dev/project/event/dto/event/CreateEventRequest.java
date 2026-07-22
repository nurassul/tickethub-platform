package dev.project.event.dto.event;

import java.time.LocalDateTime;

public record CreateEventRequest(
        String title,
        String description,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String city,
        String venueName,
        String venueAddress
) {
}
