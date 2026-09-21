package dev.project.ticket.integration.kafka.listener;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.ticket.integration.kafka.event.BookingConfirmedEvent;
import dev.project.ticket.integration.kafka.handler.BookingEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Component
public class BookingEventsListener {

    private final ObjectMapper objectMapper;
    private final BookingEventHandler bookingEventHandler;

    private static final String CONFIRMED_TYPE = "booking.confirmed";


    @KafkaListener(topics = "${app.kafka.topics.booking-events}")
    public void listen(ConsumerRecord<String, String> record) throws JsonProcessingException {

        JsonNode root = objectMapper.readTree(record.value());
        String eventType = root.path("eventType").asText();

        switch (eventType) {
            case CONFIRMED_TYPE -> {
                var event = objectMapper.readValue(
                        record.value(),
                        BookingConfirmedEvent.class
                );
                validateConfirmedEvent(event);
                bookingEventHandler.handleConfirmed(event);
                log.info("booking.confirmed handled: bookingId={}", event.payload().bookingId());
            }

            default -> log.debug("Ignored booking event: type={}, offset={}", eventType, record.offset());
        }


    }


    private void validateConfirmedEvent(BookingConfirmedEvent bookingConfirmedEvent) {
        if (bookingConfirmedEvent.eventVersion() != 1) {
            throw new IllegalStateException("'eventVersion' is not equal to 1");
        }

        if (bookingConfirmedEvent.eventId() == null) {
            throw new IllegalStateException("'eventId' must not be null");
        }

        if (!CONFIRMED_TYPE.equals(bookingConfirmedEvent.eventType())) {
            throw new IllegalStateException("'eventType' must be 'booking.confirmed'");
        }

        if (!bookingConfirmedEvent.aggregateType().equals("booking")) {
            throw new IllegalStateException("'aggregateType' must be 'booking'");
        }

        if (bookingConfirmedEvent.payload() == null) {
            throw new IllegalStateException("'payload' must not be null");
        }

        if (bookingConfirmedEvent.payload().bookingId() == null) {
            throw new IllegalStateException("'payload.bookingId' must not be null");
        }

        if (!Objects.equals(
                bookingConfirmedEvent.aggregateId(),
                bookingConfirmedEvent.payload().bookingId()
        )) {
            throw new IllegalArgumentException(
                    "'aggregateId' must be equal to 'bookingId'"
            );
        }

        if (!Objects.equals(
                bookingConfirmedEvent.correlationId(),
                bookingConfirmedEvent.payload().bookingId()
        )) {
            throw new IllegalArgumentException(
                    "'correlationId' must be equal to 'bookingId'"
            );
        }


        if (bookingConfirmedEvent.payload().eventId() == null) {
            throw new IllegalStateException("'payload.eventId' must not be null");
        }

        if (bookingConfirmedEvent.payload().seatIds() == null
                || bookingConfirmedEvent.payload().seatIds().isEmpty()
                || bookingConfirmedEvent.payload().seatIds().contains(null)
                || new HashSet<>(bookingConfirmedEvent.payload().seatIds()).size()
                != bookingConfirmedEvent.payload().seatIds().size()
        ) {
            throw new IllegalArgumentException(
                    "'seatIds' must be non-empty, non-null and unique"
            );
        }



    }

}
