package dev.project.service;


import dev.project.repository.mongodb.NotificationRepository;
import dev.project.repository.mongodb.NotificationTaskRepository;
import dev.project.repository.mongodb.entity.NotificationDocument;
import dev.project.utils.QrCodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationSenderTest {

    @Mock
    private NotificationTaskRepository notificationTaskRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private QrCodeGenerator qrCodeGenerator;

    @InjectMocks
    private NotificationSender notificationSender;


    @Test
    public void shouldNotSendEmailWhenNoTaskAvailable() {
        when(notificationTaskRepository.claimNext(any(Instant.class), eq(Duration.ofMinutes(1))))
                .thenReturn(Optional.empty());

        notificationSender.processNext();

        verifyNoInteractions(emailService, qrCodeGenerator);
    }


    @Test
    public void shouldMarkTaskSentAfterSuccessfulEmail() throws Exception {
        var notification = NotificationDocument.builder()
                .id("notification-1")
                .recipientEmail("test@example.com")
                .bookingId("booking-1")
                .ticketIds(List.of("ticket-1"))
                .lockToken("lock-1")
                .status("PROCESSING")
                .build();

        when(notificationTaskRepository.claimNext(any(Instant.class), eq(Duration.ofMinutes(1))))
                .thenReturn(Optional.of(notification));

        byte[] qrBytes = new byte[]{1, 2, 3};

        when(qrCodeGenerator.generateQrCode("ticket-1")).thenReturn(qrBytes);

        when(notificationTaskRepository.markSent(eq("notification-1"), eq("lock-1"), any(Instant.class)))
                .thenReturn(true);

        notificationSender.processNext();

        verify(emailService).sendTicketsEmail(eq("test@example.com"), eq("booking-1"), eq(Map.of("ticket-1", qrBytes)));
        verify(notificationTaskRepository).markSent(eq("notification-1"), eq("lock-1"), any(Instant.class));

    }

    @Test
    public void shouldMarkTaskFailedWhenEmailSendingFails() throws Exception {
        var notification = NotificationDocument.builder()
                .id("notification-1")
                .recipientEmail("test@example.com")
                .bookingId("booking-1")
                .ticketIds(List.of("ticket-1"))
                .lockToken("lock-1")
                .status("PROCESSING")
                .build();

        when(notificationTaskRepository.claimNext(any(Instant.class), eq(Duration.ofMinutes(1))))
                .thenReturn(Optional.of(notification));

        byte[] qrBytes = new byte[]{1, 2, 3};

        when(qrCodeGenerator.generateQrCode("ticket-1")).thenReturn(qrBytes);

        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailService)
                .sendTicketsEmail(eq("test@example.com"), eq("booking-1"), eq(Map.of("ticket-1", qrBytes)));

        when(notificationTaskRepository.markFailed(eq("notification-1"), eq("lock-1"), eq("SMTP unavailable"), any(Instant.class)))
                .thenReturn(true);

        notificationSender.processNext();

        verify(notificationTaskRepository, never()).markSent(anyString(), anyString(), any(Instant.class));
        verify(notificationTaskRepository).markFailed(eq("notification-1"), eq("lock-1"), eq("SMTP unavailable"), any(Instant.class));
    }

    @Test
    public void shouldSendAllTicketsInOneEmail() throws Exception {
        var notification = NotificationDocument.builder()
                .id("notification-1")
                .recipientEmail("test@example.com")
                .bookingId("booking-1")
                .ticketIds(List.of("ticket-1", "ticket-2", "ticket-3"))
                .lockToken("lock-1")
                .status("PROCESSING")
                .build();

        when(notificationTaskRepository.claimNext(any(Instant.class), eq(Duration.ofMinutes(1))))
                .thenReturn(Optional.of(notification));

        byte[] qrBytes1 = new byte[]{1, 2, 3};
        byte[] qrBytes2 = new byte[]{4, 5, 6};
        byte[] qrBytes3 = new byte[]{7, 8, 9};

        when(qrCodeGenerator.generateQrCode("ticket-1")).thenReturn(qrBytes1);
        when(qrCodeGenerator.generateQrCode("ticket-2")).thenReturn(qrBytes2);
        when(qrCodeGenerator.generateQrCode("ticket-3")).thenReturn(qrBytes3);

        when(notificationTaskRepository.markSent(eq("notification-1"), eq("lock-1"), any(Instant.class)))
                .thenReturn(true);

        notificationSender.processNext();

        verify(emailService).sendTicketsEmail(eq("test@example.com"), eq("booking-1"), eq(Map.of("ticket-1", qrBytes1, "ticket-2", qrBytes2, "ticket-3", qrBytes3)));
        verify(notificationTaskRepository).markSent(eq("notification-1"), eq("lock-1"), any(Instant.class));

    }


}
