package dev.project.booking.integration.kafka;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.booking.integration.kafka.event.BookingExpiredEvent;
import dev.project.booking.repository.entity.Booking;
import dev.project.booking.repository.entity.OutboxEvent;
import dev.project.booking.repository.postgresql.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingOutboxService {

    private static final String BOOKING_EXPIRED_TYPE = "booking.expired";


    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    @Value("${app.kafka.topics.booking-events}")
    private String bookingEventsTopic;


    public void saveBookingExpired(Booking booking) {
        Instant now = Instant.now();

        BookingExpiredEvent event = new BookingExpiredEvent(
                UUID.randomUUID(),
                BOOKING_EXPIRED_TYPE,
                1,
                now,
                "booking-service",
                "booking",
                booking.getId(),
                booking.getId(),
                new BookingExpiredEvent.Payload(
                        booking.getId(),
                        booking.getExpiresAt().toInstant(ZoneOffset.UTC)
                )
        );

        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize booking.expired event",
                    e
            );
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(event.eventId())
                .topic(bookingEventsTopic)
                .messageKey(booking.getId().toString())
                .eventType(event.eventType())
                .payload(payload)
                .build();

        outboxEventRepository.save(outboxEvent);
    }
}
