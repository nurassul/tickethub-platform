package dev.project.service;


import dev.project.repository.mongodb.NotificationTaskRepository;
import dev.project.utils.QrCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSender {

    private final NotificationTaskRepository notificationTaskRepository;
    private final EmailService emailService;
    private final QrCodeGenerator qrCodeGenerator;


    @Scheduled(fixedDelay = 1000)
    public void processNext() {
        var claimed = notificationTaskRepository.claimNext(Instant.now(), Duration.ofMinutes(1));
        if (claimed.isEmpty()) {
            return;
        }

        var notification = claimed.get();

        try {
            var ticketIds = notification.getTicketIds();
            Map<String, byte[]> qrCodes = new LinkedHashMap<>();

            for (String ticketId : ticketIds) {
                byte[] qrCode = qrCodeGenerator.generateQrCode(ticketId);
                qrCodes.put(ticketId, qrCode);
            }

            emailService.sendTicketsEmail(notification.getRecipientEmail(), notification.getBookingId(), qrCodes);

        } catch (Exception e) {
            var res = notificationTaskRepository.markFailed(notification.getId(), notification.getLockToken(), e.getMessage(), Instant.now().plusSeconds(30));

            if (res) {
                log.error("Error while sending email resending after 30s: id={}", notification.getId(), e);
            } else {
                log.warn("Couldn't update document with id={}", notification.getId(), e);
            }

            return;
        }

        var res = notificationTaskRepository.markSent(notification.getId(), notification.getLockToken(), Instant.now());

        if (res) {
            log.info("Email was successfully sent: id={}", notification.getId());
        } else {
            log.warn("Couldn't save SENT: notification not found by id, status PROCESSING and lockToken: id={}", notification.getId());
        }


    }

}
