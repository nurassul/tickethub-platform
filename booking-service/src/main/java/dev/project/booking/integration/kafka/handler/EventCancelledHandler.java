package dev.project.booking.integration.kafka.handler;


import dev.project.booking.api.services.EventLockService;
import dev.project.booking.integration.kafka.event.EventCancelledEvent;
import dev.project.booking.integration.kafka.service.ProcessedEventService;
import dev.project.booking.repository.entity.CancelledEvent;
import dev.project.booking.repository.postgresql.CancelledEventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Component
public class EventCancelledHandler {

    private final ProcessedEventService processedEventService;
    private final CancelledEventsRepository cancelledEventsRepository;
    private final EventLockService eventLockService;

    private static final String CONSUMER_NAME = "booking-event-events-v1";


    @Transactional
    public void handle(EventCancelledEvent event) {

        if (processedEventService.tryRegister(CONSUMER_NAME, event.eventId())) {
            eventLockService.lock(event.payload().eventId());

            var cancelledEvent = CancelledEvent.builder()
                    .eventId(event.payload().eventId())
                    .cancelledAt(event.payload().cancelledAt())
                    .build();

            cancelledEventsRepository.save(cancelledEvent);

            log.info(
                    "event.cancelled processed: eventId={}, cancelledAt={}",
                    event.payload().eventId(),
                    event.payload().cancelledAt()
            );

        } else {
            log.debug("Event already processed!");
            return;
        }


    }


}
