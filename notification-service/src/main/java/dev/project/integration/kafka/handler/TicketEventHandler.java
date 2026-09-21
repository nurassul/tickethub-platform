package dev.project.integration.kafka.handler;


import dev.project.repository.mongodb.NotificationRepository;
import dev.project.repository.mongodb.entity.NotificationDocument;
import dev.project.service.EmailService;
import dev.project.utils.QrCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventHandler {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final QrCodeGenerator qrCodeGenerator;

    private static final String TICKET_GENERATED_TYPE = "TICKET_GENERATED";


    public void handleTicketsGenerated(
            String customerEmail,
            String bookingId,
            List<String> ticketIds
    ) {
        var notification = NotificationDocument.builder()
                .type(TICKET_GENERATED_TYPE)
                .recipientEmail(customerEmail)
                .subject("Your ticket on TicketHub")
                .bookingId(bookingId)
                .ticketIds(ticketIds)
                .status("PENDING")
                .build();

        notification = notificationRepository.save(notification);

        try {
            byte[] qrCode = qrCodeGenerator.generateQrCode(ticketIds.getFirst());

            emailService.sendTicketEmail(
                    customerEmail,
                    "TicketHub - Booking #" + bookingId,
                    ticketIds.getFirst(),
                    qrCode
            );

            notification.setStatus("SENT");
            notification.setSentAt(Instant.now());
            notificationRepository.save(notification);

            log.info("Notification SENT: email={}, bookingId={}, ticketIds={}",
                    customerEmail, bookingId, ticketIds);
        } catch (Exception e) {
            notification.setStatus("FAILED");
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);

            log.error("Notification FAILED: email={}, bookingId={}", customerEmail, bookingId, e);
        }
    }



}
