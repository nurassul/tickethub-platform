package dev.project.event.utils;


import dev.project.event.dto.event.CreateEventRequest;
import dev.project.event.dto.event.EventResponse;
import dev.project.event.dto.event.UpdateEventRequest;
import dev.project.event.repository.entity.Event;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;


@Component
public class EventMapper {

    public Event toEntity(CreateEventRequest request) {
        return Event.builder()
                .title(request.title())
                .description(request.description())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .city(request.city())
                .venueName(request.venueName())
                .venueAddress(request.venueAddress())
                .build();
    }

    public EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getCity(),
                event.getVenueName(),
                event.getVenueAddress(),
                event.getStatus()
        );
    }

    public void updateEntity(Event event, UpdateEventRequest request) {
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        event.setCity(request.city());
        event.setVenueName(request.venueName());
        event.setVenueAddress(request.venueAddress());
    }

}
