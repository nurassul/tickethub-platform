package dev.project.booking.api.controllers;


import dev.project.booking.api.services.GuestBookingAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/bookings")
public class InternalBookingController {

    private final GuestBookingAccessService guestBookingAccessService;


    @PostMapping("/{bookingId}/access-check")
    public ResponseEntity<Void> checkAccess(
            @PathVariable UUID bookingId,
            @RequestHeader(value = "X-Booking-Token", required = false)
            String guestToken
    ) {
        guestBookingAccessService.requireAccess(bookingId, guestToken);

        return ResponseEntity.noContent().build();
    }
}
