package dev.project.integration.kafka.handler;


import dev.project.repository.mongodb.NotificationRepository;
import dev.project.repository.mongodb.entity.NotificationDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventHandler {

    private final NotificationRepository notificationRepository;

    private static final String TICKET_GENERATED_TYPE = "TICKET_GENERATED";


    public void handleTicketsGenerated(
            String customerEmail,
            String bookingId,
            List<String> ticketIds,
            UUID eventId
    ) {
        var notification = NotificationDocument.builder()
                .id(eventId.toString())
                .type(TICKET_GENERATED_TYPE)
                .recipientEmail(customerEmail)
                .subject("Your ticket on TicketHub")
                .bookingId(bookingId)
                .ticketIds(ticketIds)
                .status("PENDING")
                .build();

        try {
            notification = notificationRepository.insert(notification);
        } catch (DuplicateKeyException e) {
            return;
        }

        log.info("Notification sent to queue: id={}, bookingId={}", notification.getId(), bookingId);
    }



}
