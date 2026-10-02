package dev.project.ticket.feign;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(
        name = "booking-access",
        url = "${clients.booking-service.url}"
)
public interface BookingServiceClient {

    @PostMapping("/api/v1/internal/bookings/{bookingId}/access-check")
    ResponseEntity<Void> checkAccess(
            @PathVariable UUID bookingId,
            @RequestHeader(value = "X-Booking-Token", required = false)
            String guestToken
    );


}
