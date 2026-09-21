package dev.project.service;


import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;


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

            helper.setText(htmlContent,true);

            helper.addInline("qrcode", new ByteArrayResource(qrCodeImage), "image/png");

            mailSender.send(message);
            log.info("Ticket successfully send to email: {}", toEmail);
        } catch (Exception e) {
            log.error("Error while sending ticket to: {}", toEmail, e);
        }


    }

}
