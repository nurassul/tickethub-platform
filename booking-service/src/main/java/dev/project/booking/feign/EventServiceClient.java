package dev.project.booking.feign;


import dev.project.booking.dto.feign.ValidateSeatsRequest;
import dev.project.booking.dto.feign.ValidatedSeatsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "event-api",
        url = "${clients.event-service.url}"
)
public interface EventServiceClient {

    @PostMapping("/api/v1/internal/events/{eventId}/seats/validate")
    ValidatedSeatsResponse validateSeats(
            @PathVariable UUID eventId,
            @RequestBody ValidateSeatsRequest request
    );

}
