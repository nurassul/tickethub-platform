package dev.project.booking.integration.kafka.listener;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.booking.integration.kafka.event.EventCancelledEvent;
import dev.project.booking.integration.kafka.handler.EventCancelledHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventEventsListener {


    private final ObjectMapper objectMapper;
    private final EventCancelledHandler eventCancelledHandler;

    private static final String EVENT_CANCELLED_TYPE = "event.cancelled";


    @KafkaListener(
            topics = "${app.kafka.topics.event-events}",
            groupId = "${app.kafka.groups.event-events}"
    )
    public void listen(ConsumerRecord<String, String> record) throws JsonProcessingException {
        var root = objectMapper.readTree(record.value());
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("Message must be a JSON object");
        }

        String eventType = root.path("eventType").asText();
        if (eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }

        if(!EVENT_CANCELLED_TYPE.equals(eventType)) {
            log.debug(
                    "Event event ignored: type={}, topic={}, offset={}",
                    eventType,
                    record.topic(),
                    record.offset()
            );
            return;
        }

        var event = objectMapper.readValue(
                record.value(),
                EventCancelledEvent.class
        );

        validate(event);

        eventCancelledHandler.handle(event);







    }
    private void validate(EventCancelledEvent event) {
        if (event.eventVersion() != 1) {
            throw new IllegalArgumentException("'eventVersion' is not equal to 1");
        }

        if (event.eventId() == null) {
            throw new IllegalArgumentException("'eventId' must not be null");
        }

        if (!EVENT_CANCELLED_TYPE.equals(event.eventType())) {
            throw new IllegalArgumentException("'eventType' must be 'event.cancelled'");
        }

        if (!"event".equals(event.aggregateType())) {
            throw new IllegalArgumentException(
                    "'aggregateType' must be 'event'"
            );
        }

        if (event.payload() == null) {
            throw new IllegalArgumentException("'payload' must not be null");
        }

        if (event.payload().eventId() == null) {
            throw new IllegalArgumentException("'payload.eventId' must not be null");
        }



        if (!"event-service".equals(event.producer())) {
            throw new IllegalArgumentException(
                    "'producer' must be 'event-service'"
            );
        }

        if (!Objects.equals(
                event.aggregateId(),
                event.payload().eventId()
        )) {
            throw new IllegalArgumentException(
                    "'aggregateId' must be equal to 'eventId'"
            );
        }

        if (!Objects.equals(
                event.correlationId(),
                event.payload().eventId()
        )) {
            throw new IllegalArgumentException(
                    "'correlationId' must be equal to 'eventId'"
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
