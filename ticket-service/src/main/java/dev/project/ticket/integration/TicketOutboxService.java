package dev.project.ticket.integration;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.ticket.integration.kafka.event.TicketCancelledEvent;
import dev.project.ticket.repository.entity.OutboxEvent;
import dev.project.ticket.repository.entity.Ticket;
import dev.project.ticket.repository.postgresql.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TicketOutboxService {

    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;


    @Value("${app.kafka.topics.ticket-events}")
    private String ticketEventsTopic;

    private static final String TICKET_CANCELLED_TYPE = "ticket.cancelled";

    public void saveTicketCancelled(Ticket ticket) {
        Instant now = Instant.now();

        TicketCancelledEvent event = new TicketCancelledEvent(
                UUID.randomUUID(),
                TICKET_CANCELLED_TYPE,
                1,
                now,
                "ticket-service",
                "ticket",
                ticket.getId(),
                ticket.getBookingId(),
                new TicketCancelledEvent.Payload(
                        ticket.getId(),
                        ticket.getBookingId(),
                        ticket.getEventId(),
                        ticket.getSeatId(),
                        now
                )
        );

        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize ticket.cancelled event",
                    e
            );
        }


        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(event.eventId())
                .topic(ticketEventsTopic)
                .messageKey(ticket.getBookingId().toString())
                .eventType(event.eventType())
                .payload(payload)
                .build();

        outboxEventRepository.save(outboxEvent);
    }












}
