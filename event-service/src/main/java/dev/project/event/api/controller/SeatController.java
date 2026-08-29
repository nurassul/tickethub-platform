package dev.project.event.api.controller;


import dev.project.event.api.service.SeatService;
import dev.project.event.dto.seat.CreateSeatBatchRequest;
import dev.project.event.dto.seat.SeatBatchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/events")
@Slf4j
public class SeatController {

    private final SeatService seatService;


    @PostMapping("/admin/{eventID}/seats")
    public ResponseEntity<SeatBatchResponse> createSeats(
            @PathVariable UUID eventID,
            @RequestBody @Valid CreateSeatBatchRequest request
    ) {
        log.info("Called createSeats() with eventID={}", eventID);

        var response = seatService.createSeats(eventID, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{eventID}/seats")
    public ResponseEntity<SeatBatchResponse> getSeatsByEventId(
            @PathVariable UUID eventID
    ) {
        log.info("Called getSeatsByEventId() with eventID={}", eventID);

        var response = seatService.getSeatsByEventId(eventID);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

}
