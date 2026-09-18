package dev.project.event.kafka.event;

import dev.project.event.repository.entity.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventSyncMessage(
        UUID id,
        String title,
        String description,
        String venueName,
        String city,
        EventStatus status,
        LocalDateTime startsAt
) {
}
