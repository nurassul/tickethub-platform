package dev.project.event.api.service.impl;

import dev.project.event.api.service.EventService;
import dev.project.event.dto.event.CreateEventRequest;
import dev.project.event.dto.event.EventResponse;
import dev.project.event.dto.event.UpdateEventRequest;
import dev.project.event.repository.EventRepository;
import dev.project.event.repository.SeatRepository;
import dev.project.event.repository.entity.Event;
import dev.project.event.repository.entity.enums.EventStatus;
import dev.project.event.utils.EventMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@RequiredArgsConstructor
@Service
@Transactional
public class EventServiceImpl implements EventService {


    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final EventMapper mapper;


    @Override
    public EventResponse createEvent(CreateEventRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new IllegalArgumentException(
                    "'endsAt' must be after 'startsAt'"
            );
        }

        var eventEntityToSave = mapper.toEntity(request);

        var savedEvent = eventRepository.save(eventEntityToSave);
        return mapper.toResponse(savedEvent);
    }

    public EventResponse findEventById(UUID eventID) {
        Event event = getEventById(eventID);

        return mapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getEvents(
            EventStatus status,
            Pageable pageable
    ) {
        Page<Event> events = eventRepository.findAllByStatus(status, pageable);

        return events.map(mapper::toResponse);
    }

    @Override
    public EventResponse updateEvent(UUID eventID, UpdateEventRequest request) {
        Event event = getEventById(eventID);

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only draft event can be edited"
            );
        }

        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new IllegalArgumentException(
                    "'endsAt' must be after 'startsAt'"
            );
        }

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        event.setCity(request.city());
        event.setVenueName(request.venueName());
        event.setVenueAddress(request.venueAddress());

        return mapper.toResponse(event);
    }


    @Override
    public EventResponse publishEvent(UUID eventID) {
        Event event = getEventById(eventID);

        if (event.getStatus() == EventStatus.PUBLISHED) {
            throw new IllegalStateException("You can't publish event which already PUBLISHED!");
        }

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalStateException("You can't publish event which was CANCELLED!");
        }

        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new IllegalStateException("You can't publish event which was COMPLETED!");
        }

        if (!seatRepository.existsByEventId(eventID)) {
            throw new IllegalStateException("This event has no seats!");
        }

        event.setStatus(EventStatus.PUBLISHED);

        return mapper.toResponse(event);
    }

    @Override
    public EventResponse cancelEvent(UUID eventID) {
        Event event = getEventById(eventID);

        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new IllegalStateException("You can't 'CANCEL' event which was 'COMPLETED'");
        }

        if (event.getStatus() != EventStatus.CANCELLED) {
            event.setStatus(EventStatus.CANCELLED);
        }

        return mapper.toResponse(event);
    }

    private Event getEventById(UUID eventID) {
        return eventRepository.findById(eventID)
                .orElseThrow(() -> new EntityNotFoundException("Event was not found by id=" + eventID));
    }

}
