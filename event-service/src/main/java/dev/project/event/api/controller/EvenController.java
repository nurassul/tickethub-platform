package dev.project.event.api.controller;


import dev.project.event.api.service.EventService;
import dev.project.event.dto.event.CreateEventRequest;
import dev.project.event.dto.event.EventResponse;
import dev.project.event.repository.entity.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/events")
@Slf4j
public class EvenController {

    private final EventService eventService;


    @PostMapping()
    public ResponseEntity<EventResponse> createEvent(
            @RequestBody CreateEventRequest request
    ) {
        log.info("Called createEvent()");

        var response = eventService.createEvent(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{eventID/publish}")
    public ResponseEntity<EventResponse> publishEvent(
            @PathVariable UUID eventID
    ) {
        log.info("Called publishEvent() with id={}", eventID);

        var response = eventService.publishEvent(eventID);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/{eventID/cancel}")
    public ResponseEntity<EventResponse> cancelEvent(
            @PathVariable UUID eventID
    ) {
        log.info("Called cancelEvent() with id={}", eventID);

        var response = eventService.cancelEvent(eventID);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }


    @GetMapping("/{eventID}")
    public ResponseEntity<EventResponse> getEventById(
            @PathVariable UUID eventID
    ) {
        log.info("Called getEventById() with id={}", eventID);

        var response = eventService.findTaskById(eventID);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping
    public Page<EventResponse> getEvents(
            @RequestParam EventStatus status,
            @PageableDefault(
                    size = 20,
                    sort = "startsAt",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return eventService.getEvents(status, pageable);
    }

    @GetMapping
    public Page<EventResponse> getPublishedEvents(
            @PageableDefault(
                    size = 20,
                    sort = "startsAt",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return eventService.getEvents(EventStatus.PUBLISHED, pageable);
    }

}
