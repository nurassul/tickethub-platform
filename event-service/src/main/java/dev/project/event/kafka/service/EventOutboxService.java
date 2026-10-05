package dev.project.event.kafka.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.event.kafka.event.EventCancelledEvent;
import dev.project.event.repository.entity.OutboxEvent;
import dev.project.event.repository.postgresql.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventOutboxService {

    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    private static final String EVENT_CANCELLED_TYPE = "event.cancelled";



    @Value("${app.kafka.topics.event-events:tickethub.event.events.v1}")
    private String eventEventsTopic;


    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEventCancelled(UUID eventId) {
        Instant now = Instant.now();

        EventCancelledEvent event = new EventCancelledEvent(
                UUID.randomUUID(),
                EVENT_CANCELLED_TYPE,
                1,
                now,
                "event-service",
                "event",
                eventId,
                eventId,
                new EventCancelledEvent.Payload(
                        eventId,
                        now
                )
        );

        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize event.cancelled event",
                    e
            );
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(event.eventId())
                .topic(eventEventsTopic)
                .messageKey(eventId.toString())
                .eventType(event.eventType())
                .payload(payload)
                .build();

        outboxEventRepository.save(outboxEvent);

    }










}
