package dev.project.booking.utils;


import dev.project.booking.api.services.BookingExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingExpirationJob {

    private final BookingExpirationService expirationService;

    @Scheduled(fixedDelay = 30_000)
    public void expireBookings() {
        expirationService.expireBookings();
    }

}
