package dev.project.booking.utils;


import dev.project.booking.api.services.BookingPersistenceService;
import dev.project.booking.repository.entity.enums.BookingStatus;
import dev.project.booking.repository.postgresql.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class EventCancellationJob {

    private final BookingRepository bookingRepository;
    private final BookingPersistenceService bookingPersistenceService;

    @Scheduled(fixedDelay = 5000)
    public void cancelBookingsForCancelledEvents() {
        var bookingIds = bookingRepository.findBookingIdsForCancelledEvents(
                List.of(BookingStatus.HOLD, BookingStatus.CONFIRMED, BookingStatus.PARTIALLY_CANCELLED),
                PageRequest.of(0, 100)
        );

        for (UUID bookingId : bookingIds) {
            try {
                bookingPersistenceService.cancelForCancelledEvent(bookingId);
            } catch (Exception e) {
                log.error("Wrong while cancelling: bookingId={}", bookingId, e);
            }

        }
    }

}
