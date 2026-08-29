package dev.project.event.api.controller;


import dev.project.event.api.service.SeatService;
import dev.project.event.dto.seat.booking.ValidateSeatsRequest;
import dev.project.event.dto.seat.booking.ValidatedSeatsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/events")
public class InternalEventController {

    private final SeatService seatService;

    @PostMapping("/{eventId}/seats/validate")
    public ValidatedSeatsResponse validateSeats(
            @PathVariable UUID eventId,
            @Valid @RequestBody ValidateSeatsRequest request
    ) {
        return seatService.validateSeats(eventId, request);
    }
}
