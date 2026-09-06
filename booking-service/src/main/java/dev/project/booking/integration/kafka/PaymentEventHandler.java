package dev.project.booking.integration.kafka;


import dev.project.booking.api.services.BookingPersistenceService;
import dev.project.booking.dto.BookingData;
import dev.project.booking.integration.kafka.event.PaymentEvent;
import dev.project.booking.redis.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


@RequiredArgsConstructor
@Slf4j
@Component
public class PaymentEventHandler {


    private static final String FAILED_TYPE = "payment.failed";
    private static final String SUCCEEDED_TYPE = "payment.succeeded";

    private final ProcessedEventService processedEventService;
    private final BookingPersistenceService bookingPersistenceService;
    private final SeatHoldService seatHoldService;


    @Transactional
    public void handleEvent(PaymentEvent paymentEvent) {
        if (processedEventService.tryRegister(paymentEvent.eventId())) {

            switch (paymentEvent.eventType()) {
                case SUCCEEDED_TYPE -> {
                    BookingData bookingData = bookingPersistenceService.confirm(paymentEvent.payload().bookingId());
                    log.info("new payment succeeded event: eventId={}, bookingId={}, seatIds = {}",
                            paymentEvent.eventId(),
                            bookingData.bookingId(),
                            bookingData.seatIds()
                    );

                    runAfterCommit(() -> {
                        seatHoldService.markSold(
                                bookingData.bookingId(),
                                bookingData.eventId(),
                                bookingData.seatIds()
                        );
                    });

                }

                case FAILED_TYPE -> {
                    BookingData bookingData = bookingPersistenceService.failPayment(paymentEvent.payload().bookingId());
                    log.info("new payment failed event: eventId={}, bookingId={}, seatIds = {}",
                            paymentEvent.eventId(),
                            bookingData.bookingId(),
                            bookingData.seatIds()
                    );

                    runAfterCommit(() -> {
                        seatHoldService.release(
                                bookingData.bookingId(),
                                bookingData.eventId(),
                                bookingData.seatIds());
                    });
                }
            }

        } else {
            log.debug("Event already processed!");
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
