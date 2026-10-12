package dev.project.service;


import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.LinkedHashMap;
import java.util.Map;


@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendTicketEmail(
            String toEmail,
            String eventTitle,
            String ticketId,
            byte[] qrCodeImage
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Your ticket for event: " + eventTitle);
            helper.setFrom("noreply@tickethub.com");

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; text-align: center;">
                        <h1>Thanks for booking! 🎫</h1>
                        <p>Your ticket for <b>%s</b> successfully purchased.</p>
                        <p>Show this QR:</p>
                        <img src='cid:qrcode' alt='Ticket QR Code' style='border: 2px solid #000; border-radius: 10px;'/>
                        <p style="color: gray; font-size: 12px;">Ticket ID: %s</p>
                    </div>
                    """.formatted(eventTitle, ticketId);

            helper.setText(htmlContent, true);

            helper.addInline("qrcode", new ByteArrayResource(qrCodeImage), "image/png");

            mailSender.send(message);
            log.info("Ticket successfully send to email: {}", toEmail);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send ticket email", e);
        }


    }


    public void sendTicketsEmail(String toEmail, String bookingId, Map<String, byte[]> qrCodes) {
        try {
            if (qrCodes == null || qrCodes.isEmpty()) {
                throw new IllegalArgumentException("At least one ticket QR code is required");
            }

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("TicketHub - Tickets for booking #" + bookingId);
            helper.setFrom("noreply@tickethub.com");

            StringBuilder html = new StringBuilder();
            html.append("""
                    <html><body style="font-family: Arial, sans-serif; color: #222;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 24px;">
                        <h1>Your TicketHub tickets</h1>
                        <p>Thank you for your booking!</p>
                        <p>Booking ID: <strong>%s</strong></p>
                        <p>Each ticket has its own QR code. Show the corresponding code at the entrance.</p>
                    """.formatted(HtmlUtils.htmlEscape(bookingId)));

            Map<String, byte[]> inlineImages = new LinkedHashMap<>();
            for (var entry : qrCodes.entrySet()) {
                String ticketId = entry.getKey();
                byte[] qrCode = entry.getValue();
                if (ticketId == null || ticketId.isBlank() || qrCode == null || qrCode.length == 0) {
                    throw new IllegalArgumentException("Each ticket must have an ID and a QR code image");
                }

                String contentId = "qr-" + inlineImages.size();
                inlineImages.put(contentId, qrCode);
                html.append("""
                        <div style="margin: 24px 0; padding: 16px; border: 1px solid #ddd; text-align: center;">
                            <p>Ticket ID: <strong>%s</strong></p>
                            <img src="cid:%s" alt="Ticket QR code" width="300" height="300"
                                 style="max-width: 100%%; height: auto;"/>
                        </div>
                        """.formatted(HtmlUtils.htmlEscape(ticketId), contentId));
            }
            html.append("</div></body></html>");

            helper.setText(html.toString(), true);
            for (var entry : inlineImages.entrySet()) {
                helper.addInline(entry.getKey(), new ByteArrayResource(entry.getValue()), "image/png");
            }

            mailSender.send(message);
            log.info("Tickets email sent: bookingId={}, ticketCount={}", bookingId, qrCodes.size());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send tickets email", e);
        }
    }
}
