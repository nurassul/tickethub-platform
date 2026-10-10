package dev.project.booking.integration.kafka.handler;


import dev.project.booking.api.services.BookingPersistenceService;
import dev.project.booking.dto.BookingData;
import dev.project.booking.integration.kafka.event.PaymentEvent;
import dev.project.booking.integration.kafka.service.BookingOutboxService;
import dev.project.booking.integration.kafka.service.ProcessedEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@RequiredArgsConstructor
@Slf4j
@Component
public class PaymentEventHandler {


    private static final String FAILED_TYPE = "payment.failed";
    private static final String SUCCEEDED_TYPE = "payment.succeeded";
    private static final String CONSUMER_NAME = "booking-payment-events-v1";

    private final ProcessedEventService processedEventService;
    private final BookingPersistenceService bookingPersistenceService;
    private final BookingOutboxService bookingOutboxService;


    @Transactional
    public void handleEvent(PaymentEvent paymentEvent) {
        if (processedEventService.tryRegister(CONSUMER_NAME, paymentEvent.eventId())) {

            switch (paymentEvent.eventType()) {
                case SUCCEEDED_TYPE -> {

                    var result = bookingPersistenceService.confirmPayment(
                            paymentEvent.payload().bookingId(),
                            paymentEvent.payload().paymentId(),
                            paymentEvent.payload().paidAt(),
                            paymentEvent.payload().amountMinor(),
                            paymentEvent.payload().currency()
                    );

                    switch (result.outcome()) {
                        case CONFIRMED -> {
                            bookingOutboxService.saveBookingConfirmed(result.bookingData());

                            log.info("new payment succeeded event: eventId={}, bookingId={}, seatIds = {}",
                                    paymentEvent.eventId(),
                                    result.bookingData().bookingId(),
                                    result.bookingData().seatIds()
                            );

                        }

                        case ALREADY_PROCESSED -> {
                            log.debug("payment already processed: bookingId={}, paymentId={}",
                                    paymentEvent.payload().bookingId(),
                                    paymentEvent.payload().paymentId()
                            );
                        }

                        case REJECTED -> {
                            bookingOutboxService.saveBookingPaymentRejected(
                                    paymentEvent.payload().bookingId(),
                                    paymentEvent.payload().paymentId(),
                                    result.rejectionReason()
                            );
                            log.warn("payment rejected: bookingId={}, paymentId={}, reason={}",
                                    paymentEvent.payload().bookingId(),
                                    paymentEvent.payload().paymentId(),
                                    result.rejectionReason()
                            );
                        }
                    }
                }

                case FAILED_TYPE -> {
                    BookingData bookingData = bookingPersistenceService.failPayment(paymentEvent.payload().bookingId());
                    log.info("new payment failed event: eventId={}, bookingId={}, seatIds = {}",
                            paymentEvent.eventId(),
                            bookingData.bookingId(),
                            bookingData.seatIds()
                    );
                }
            }

        } else {
            log.debug("Event already processed!");
        }

    }


}
