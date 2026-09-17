package dev.project.booking.integration.kafka.listener;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.booking.integration.kafka.event.TicketCancelledEvent;
import dev.project.booking.integration.kafka.handler.TicketCancelledHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventsListener {


    private final ObjectMapper objectMapper;
    private final TicketCancelledHandler ticketCancelledHandler;


    private static final String TICKET_CANCELLED_TYPE = "ticket.cancelled";


    @KafkaListener(
            topics = "${app.kafka.topics.ticket-events}",
            groupId = "${app.kafka.groups.ticket-events}"
    )
    public void listen(ConsumerRecord<String, String> record)
            throws JsonProcessingException {


        JsonNode root = objectMapper.readTree(record.value());


        String eventType = root.path("eventType").asText();

        if (!TICKET_CANCELLED_TYPE.equals(eventType)) {
            log.debug(
                    "Ticket event ignored: type={}, topic={}, offset={}",
                    eventType,
                    record.topic(),
                    record.offset()
            );
            return;
        }

        TicketCancelledEvent event = objectMapper.readValue(
                record.value(),
                TicketCancelledEvent.class
        );

        validate(event);

        ticketCancelledHandler.handle(event);

        log.info(
                "ticket.cancelled received: eventId={}, ticketId={}, bookingId={}, seatId={}",
                event.eventId(),
                event.payload().ticketId(),
                event.payload().bookingId(),
                event.payload().seatId()
        );

    }

    private void validate(TicketCancelledEvent event) {
        if (event.eventVersion() != 1) {
            throw new IllegalStateException("'eventVersion' is not equal to 1");
        }

        if (event.eventId() == null) {
            throw new IllegalStateException("'eventId' must not be null");
        }

        if (!TICKET_CANCELLED_TYPE.equals(event.eventType())) {
            throw new IllegalStateException("'eventType' must be 'ticket.cancelled'");
        }

        if (!"ticket".equals(event.aggregateType())) {
            throw new IllegalArgumentException(
                    "'aggregateType' must be 'ticket'"
            );
        }

        if (event.payload() == null) {
            throw new IllegalStateException("'payload' must not be null");
        }

        if (event.payload().ticketId() == null) {
            throw new IllegalStateException("'payload.ticketId' must not be null");
        }


        if (event.payload().eventId() == null) {
            throw new IllegalStateException("'payload.eventId' must not be null");
        }

        if (event.payload().seatId() == null) {
            throw new IllegalStateException("'payload.seatId' must not be null");
        }

        if (event.payload().bookingId() == null) {
            throw new IllegalArgumentException(
                    "'payload.bookingId' must not be null"
            );
        }

        if (!"ticket-service".equals(event.producer())) {
            throw new IllegalArgumentException(
                    "'producer' must be 'ticket-service'"
            );
        }

        if (!Objects.equals(
                event.aggregateId(),
                event.payload().ticketId()
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

        if (event.occurredAt() == null) {
            throw new IllegalArgumentException(
                    "'occurredAt' must not be null"
            );
        }

        if (event.payload().cancelledAt() == null) {
            throw new IllegalArgumentException(
                    "'payload.cancelledAt' must not be null"
            );
        }


    }


}
