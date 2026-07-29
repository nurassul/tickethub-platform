package dev.project.booking.utils;


import dev.project.booking.api.services.BookingExpirationService;
import dev.project.booking.dto.BookingData;
import dev.project.booking.redis.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingExpirationJob {

    private final BookingExpirationService expirationService;
    private final SeatHoldService seatHoldService;


    @Scheduled(fixedDelay = 30_000)
    public void expireBookings() {
        List<BookingData> expiredBookings =
                expirationService.expireBookings();

        expiredBookings.forEach(booking ->
                seatHoldService.release(
                        booking.bookingId(),
                        booking.eventId(),
                        booking.seatIds()
                )
        );
    }

}
