package tiameds.com.tiameds.services.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Sends OTP email using Spring Mail (SMTP)
     */
    public void sendOtpEmail(String to, String otp) {
        log.info("Attempting to send OTP email to: {}", to);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Your Login OTP");

            String bodyText = String.format("Your login OTP is: %s. Valid for 5 minutes.", otp);
            String bodyHtml = String.format(
                "<html><body><h2>Your Login OTP</h2><p>Your login OTP is: <strong>%s</strong></p><p>Valid for 5 minutes.</p></body></html>",
                otp
            );

            helper.setText(bodyText, bodyHtml);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage(), e);
            throw new EmailException("Failed to send OTP email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending OTP email to {}: {}", to, e.getMessage(), e);
            throw new EmailException("Unexpected error sending OTP email: " + e.getMessage(), e);
        }
    }

    /**
     * Custom exception for email-related errors
     */
    public static class EmailException extends RuntimeException {
        public EmailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

