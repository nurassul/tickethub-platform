package dev.project.event.api.service;


import dev.project.event.dto.event.CreateEventRequest;
import dev.project.event.dto.event.EventResponse;
import dev.project.event.dto.event.UpdateEventRequest;
import dev.project.event.repository.entity.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EventService {


    EventResponse createEvent(CreateEventRequest request);
    EventResponse publishEvent(UUID eventID);
    EventResponse cancelEvent(UUID eventID);
    EventResponse findEventById(UUID eventID);
    Page<EventResponse> getEvents(EventStatus status, Pageable pageable);

    EventResponse updateEvent(UUID eventID, UpdateEventRequest request);
}
