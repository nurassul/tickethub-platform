package dev.project.ticket.integration.kafka.listener;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.ticket.integration.kafka.event.BookingCancelledEvent;
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
    private static final String CANCELLED_TYPE = "booking.cancelled";


    @KafkaListener(topics = "${app.kafka.topics.booking-events}")
    public void listen(ConsumerRecord<String, String> record) throws JsonProcessingException {

        JsonNode root = objectMapper.readTree(record.value());
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("Message must be a JSON object");
        }

        String eventType = root.path("eventType").asText();
        if (eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }

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

            case CANCELLED_TYPE ->  {
                var event = objectMapper.readValue(
                        record.value(),
                        BookingCancelledEvent.class
                );
                validateCancelledEvent(event);
                bookingEventHandler.handleCancelled(event);
                log.info("booking.cancelled handled: bookingId={}", event.payload().bookingId());
            }

            default -> log.debug("Ignored booking event: type={}, offset={}", eventType, record.offset());
        }


    }


    private void validateConfirmedEvent(BookingConfirmedEvent bookingConfirmedEvent) {
        if (bookingConfirmedEvent.eventVersion() != 1) {
            throw new IllegalArgumentException("'eventVersion' is not equal to 1");
        }

        if (bookingConfirmedEvent.eventId() == null) {
            throw new IllegalArgumentException("'eventId' must not be null");
        }

        if (!CONFIRMED_TYPE.equals(bookingConfirmedEvent.eventType())) {
            throw new IllegalArgumentException("'eventType' must be 'booking.confirmed'");
        }

        if (!bookingConfirmedEvent.aggregateType().equals("booking")) {
            throw new IllegalArgumentException("'aggregateType' must be 'booking'");
        }

        if (bookingConfirmedEvent.payload() == null) {
            throw new IllegalArgumentException("'payload' must not be null");
        }

        if (bookingConfirmedEvent.payload().bookingId() == null) {
            throw new IllegalArgumentException("'payload.bookingId' must not be null");
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

    private void validateCancelledEvent(BookingCancelledEvent event) {
        if (event.eventVersion() != 1) {
            throw new IllegalArgumentException("'eventVersion' is not equal to 1");
        }

        if (event.eventId() == null) {
            throw new IllegalArgumentException("'eventId' must not be null");
        }

        if (!CANCELLED_TYPE.equals(event.eventType())) {
            throw new IllegalArgumentException("'eventType' must be 'booking.cancelled'");
        }

        if (!"booking-service".equals(event.producer())) {
            throw new IllegalArgumentException("'producer' must be 'booking-service'");
        }

        if (!"booking".equals(event.aggregateType())) {
            throw new IllegalArgumentException("'aggregateType' must be 'booking'");
        }

        if (event.payload() == null) {
            throw new IllegalArgumentException("'payload' must not be null");
        }

        if (event.payload().bookingId() == null) {
            throw new IllegalArgumentException("'payload.bookingId' must not be null");
        }

        if (event.payload().cancelledAt() == null) {
            throw new IllegalArgumentException("'cancelledAt' must not be null");
        }

        if (event.occurredAt() == null) {
            throw new IllegalArgumentException("'occurredAt' must not be null");
        }

        if (!Objects.equals(
                event.aggregateId(),
                event.payload().bookingId()
        )) {
            throw new IllegalArgumentException(
                    "'aggregateId' must be equal to 'bookingId'"
            );
        }

        if (!Objects.equals(
                event.correlationId(),
                event.payload().bookingId()
        )) {
            throw new IllegalArgumentException(
                    "'correlationId' must be equal to 'bookingId'"
            );
        }
    }

}
