package dev.project.booking.integration.kafka.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.project.booking.dto.BookingData;
import dev.project.booking.integration.kafka.event.BookingCancelledEvent;
import dev.project.booking.integration.kafka.event.BookingConfirmedEvent;
import dev.project.booking.integration.kafka.event.BookingExpiredEvent;
import dev.project.booking.integration.kafka.event.BookingPaymentRejectedEvent;
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
    private static final String BOOKING_CONFIRMED_TYPE = "booking.confirmed";
    private static final String BOOKING_PAYMENT_REJECTED_TYPE = "booking.payment-rejected";
    private static final String BOOKING_CANCELLED_TYPE = "booking.cancelled";


    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    @Value("${app.kafka.topics.booking-events}")
    private String bookingEventsTopic;

    public void saveBookingConfirmed(BookingData data) {
        Instant now = Instant.now();

        BookingConfirmedEvent event = new BookingConfirmedEvent(
                UUID.randomUUID(),
                BOOKING_CONFIRMED_TYPE,
                1,
                now,
                "booking-service",
                "booking",
                data.bookingId(),
                data.bookingId(),
                new BookingConfirmedEvent.Payload(
                        data.bookingId(),
                        data.eventId(),
                        data.seatIds(),
                        data.customerEmail()
                )
        );

        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize booking.confirmed event",
                    e
            );
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(event.eventId())
                .topic(bookingEventsTopic)
                .messageKey(data.bookingId().toString())
                .eventType(event.eventType())
                .payload(payload)
                .build();

        outboxEventRepository.save(outboxEvent);
    }


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



    public void saveBookingPaymentRejected(
            UUID bookingId,
            UUID paymentId,
            String reason
    ) {
        Instant now = Instant.now();

        BookingPaymentRejectedEvent event = new BookingPaymentRejectedEvent(
                UUID.randomUUID(),
                BOOKING_PAYMENT_REJECTED_TYPE,
                1,
                now,
                "booking-service",
                "booking",
                bookingId,
                bookingId,
                new BookingPaymentRejectedEvent.Payload(
                        bookingId,
                        paymentId,
                        reason
                )
        );

        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize booking.payment-rejected event",
                    e
            );
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(event.eventId())
                .topic(bookingEventsTopic)
                .messageKey(bookingId.toString())
                .eventType(event.eventType())
                .payload(payload)
                .build();

        outboxEventRepository.save(outboxEvent);
    }


    public void saveBookingCancelled(UUID bookingId) {
        Instant now = Instant.now();

        BookingCancelledEvent event = new BookingCancelledEvent(
                UUID.randomUUID(),
                BOOKING_CANCELLED_TYPE,
                1,
                now,
                "booking-service",
                "booking",
                bookingId,
                bookingId,
                new BookingCancelledEvent.Payload(
                        bookingId,
                        now
                )
        );

        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize booking.cancelled event",
                    e
            );
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(event.eventId())
                .topic(bookingEventsTopic)
                .messageKey(bookingId.toString())
                .eventType(event.eventType())
                .payload(payload)
                .build();

        outboxEventRepository.save(outboxEvent);



    }



}
