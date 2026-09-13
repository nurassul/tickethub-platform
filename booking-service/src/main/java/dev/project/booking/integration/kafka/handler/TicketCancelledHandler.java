package dev.project.booking.integration.kafka.handler;


import dev.project.booking.api.services.BookingPersistenceService;
import dev.project.booking.integration.kafka.event.TicketCancelledEvent;
import dev.project.booking.integration.kafka.service.ProcessedEventService;
import dev.project.booking.redis.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class TicketCancelledHandler {

    private final ProcessedEventService processedEventService;
    private final BookingPersistenceService bookingPersistenceService;
    private final SeatHoldService seatHoldService;


    private static final String CONSUMER_NAME = "booking-ticket-events-v1";


    @Transactional
    public void handle(TicketCancelledEvent event) {

        if (processedEventService.tryRegister(CONSUMER_NAME, event.eventId())) {

            bookingPersistenceService.releaseSoldSeat(
                    event.payload().bookingId(),
                    event.payload().eventId(),
                    event.payload().seatId()
            );

            runAfterCommit(() -> {
                seatHoldService.releaseSold(
                        event.payload().bookingId(),
                        event.payload().eventId(),
                        List.of(event.payload().seatId())
                );
            });


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


    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    }
            );
        } else {
            action.run();
        }
    }

}
