package dev.project.integration.kafka.handler;


import dev.project.repository.mongodb.entity.NotificationDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Import(TicketEventHandler.class)
@Testcontainers
public class TicketEventHandlerTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Autowired
    private TicketEventHandler ticketEventHandler;

    @Autowired
    public MongoTemplate mongoTemplate;


    @Test
    public void shouldNotCreateDuplicateNotificationForSameEvent() {
        var eventId = UUID.randomUUID();
        var customerEmail = "test@example.com";
        var bookingId = "booking-1";
        var ticketIds = List.of("ticket-1", "ticket-2");

        ticketEventHandler.handleTicketsGenerated(customerEmail, bookingId, ticketIds, eventId);
        ticketEventHandler.handleTicketsGenerated(customerEmail, bookingId, ticketIds, eventId);

        var count = mongoTemplate.count(new Query(), NotificationDocument.class);
        assertEquals(1L, count);

        var saved = mongoTemplate.findById(eventId.toString(), NotificationDocument.class);

        assertNotNull(saved);

        assertEquals("PENDING", saved.getStatus());
        assertEquals(customerEmail, saved.getRecipientEmail());
        assertEquals(bookingId, saved.getBookingId());
        assertEquals(ticketIds, saved.getTicketIds());
    }


    @Test
    public void shouldNotResetSentNotificationOnDuplicateEvent(){
        var eventId = UUID.randomUUID();
        var customerEmail = "test@example.com";
        var bookingId = "booking-1";
        var ticketIds = List.of("ticket-1", "ticket-2");
        var sentAt = Instant.parse("2026-10-13T10:00:00Z");

        var notification = NotificationDocument.builder()
                .id(eventId.toString())
                .recipientEmail(customerEmail)
                .bookingId(bookingId)
                .ticketIds(ticketIds)
                .status("SENT")
                .attempts(1)
                .sentAt(sentAt)
                .build();

        mongoTemplate.insert(notification);

        ticketEventHandler.handleTicketsGenerated(customerEmail, bookingId, ticketIds, eventId);

        var count = mongoTemplate.count(new Query(), NotificationDocument.class);
        assertEquals(1L, count);

        var saved = mongoTemplate.findById(eventId.toString(), NotificationDocument.class);

        assertNotNull(saved);

        assertEquals("SENT", saved.getStatus());
        assertEquals(sentAt, saved.getSentAt());
        assertEquals(1, saved.getAttempts());
        assertNull(saved.getNextAttemptAt());
    }


    @BeforeEach
    void setUp() {
        mongoTemplate.remove(new Query(), NotificationDocument.class);
    }



}
