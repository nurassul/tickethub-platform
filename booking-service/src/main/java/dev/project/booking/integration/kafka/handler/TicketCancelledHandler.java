package dev.project.booking.integration.kafka.handler;


import dev.project.booking.api.services.BookingPersistenceService;
import dev.project.booking.integration.kafka.event.TicketCancelledEvent;
import dev.project.booking.integration.kafka.service.ProcessedEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Component
public class TicketCancelledHandler {

    private final ProcessedEventService processedEventService;
    private final BookingPersistenceService bookingPersistenceService;

    private static final String CONSUMER_NAME = "booking-ticket-events-v1";


    @Transactional
    public void handle(TicketCancelledEvent event) {

        if (processedEventService.tryRegister(CONSUMER_NAME, event.eventId())) {

            bookingPersistenceService.releaseSoldSeat(
                    event.payload().bookingId(),
                    event.payload().eventId(),
                    event.payload().seatId()
            );


            log.info(
                    "ticket.cancelled processed: eventId={}, bookingId={}, seatId={}",
                    event.eventId(),
                    event.payload().bookingId(),
                    event.payload().seatId()
            );


        } else {
            log.debug("Event already processed!");
            return;
        }


    }

}
