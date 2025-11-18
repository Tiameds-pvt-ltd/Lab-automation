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
     * Sends password reset email with reset link
     * @param to Email address to send to
     * @param resetToken The password reset token
     * @param resetUrl Frontend reset password URL
     * @param backendBaseUrl Backend base URL (not used, kept for compatibility)
     */
    public void sendPasswordResetEmail(String to, String resetToken, String resetUrl, String backendBaseUrl) {
        log.info("Attempting to send password reset email to: {}", to);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Password Reset Request");

            // URL encode the token to handle special characters
            String encodedToken = java.net.URLEncoder.encode(resetToken, java.nio.charset.StandardCharsets.UTF_8);
            
            // Construct frontend URL with token - user will enter password on frontend
            // Frontend should call backend /auth/reset-password endpoint with the token
            String resetLink = resetUrl + "?token=" + encodedToken;
            
            String bodyText = String.format(
                "You requested a password reset. Click the link below to reset your password:\n\n%s\n\n" +
                "This link will expire in 15 minutes. If you did not request this, please ignore this email.",
                resetLink
            );
            
            String bodyHtml = String.format(
                "<html><body>" +
                "<h2>Password Reset Request</h2>" +
                "<p>You requested a password reset. Click the link below to reset your password:</p>" +
                "<p><a href=\"%s\" style=\"background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;\">Reset Password</a></p>" +
                "<p>Or copy and paste this link into your browser:</p>" +
                "<p style=\"word-break: break-all;\">%s</p>" +
                "<p><strong>This link will expire in 15 minutes.</strong></p>" +
                "<p>If you did not request this password reset, please ignore this email.</p>" +
                "</body></html>",
                resetLink, resetLink
            );

            helper.setText(bodyText, bodyHtml);

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage(), e);
            throw new EmailException("Failed to send password reset email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending password reset email to {}: {}", to, e.getMessage(), e);
            throw new EmailException("Unexpected error sending password reset email: " + e.getMessage(), e);
        }
    }

    /**
     * Sends password reset confirmation email
     */
    public void sendPasswordResetConfirmationEmail(String to) {
        log.info("Attempting to send password reset confirmation email to: {}", to);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Password Reset Successful");

            String bodyText = "Your password has been successfully reset. If you did not make this change, please contact support immediately.";
            String bodyHtml = String.format(
                "<html><body>" +
                "<h2>Password Reset Successful</h2>" +
                "<p>Your password has been successfully reset.</p>" +
                "<p><strong>If you did not make this change, please contact support immediately.</strong></p>" +
                "</body></html>"
            );

            helper.setText(bodyText, bodyHtml);

            mailSender.send(message);
            log.info("Password reset confirmation email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send password reset confirmation email to {}: {}", to, e.getMessage(), e);
            throw new EmailException("Failed to send password reset confirmation email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending password reset confirmation email to {}: {}", to, e.getMessage(), e);
            throw new EmailException("Unexpected error sending password reset confirmation email: " + e.getMessage(), e);
        }
    }

    /**
     * Sends onboarding verification email with verification link
     * @param to Email address to send to
     * @param verificationToken The verification token
     * @param verificationUrl Frontend verification URL (will redirect to onboarding form)
     */
    public void sendVerificationEmail(String to, String verificationToken, String verificationUrl) {
        log.info("Attempting to send verification email to: {}", to);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Complete Your Onboarding");

            // URL encode the token to handle special characters
            String encodedToken = java.net.URLEncoder.encode(verificationToken, java.nio.charset.StandardCharsets.UTF_8);
            
            // Construct verification URL with token
            String verificationLink = verificationUrl + "?token=" + encodedToken;
            
            String bodyText = String.format(
                "Welcome! Please click the link below to complete your onboarding:\n\n%s\n\n" +
                "This link will expire in 15 minutes. If you did not request this, please ignore this email.",
                verificationLink
            );
            
            String bodyHtml = String.format(
                "<html><body>" +
                "<h2>Welcome to Lab Automation</h2>" +
                "<p>Please click the link below to complete your onboarding:</p>" +
                "<p><a href=\"%s\" style=\"background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;\">Complete Onboarding</a></p>" +
                "<p>Or copy and paste this link into your browser:</p>" +
                "<p style=\"word-break: break-all;\">%s</p>" +
                "<p><strong>This link will expire in 15 minutes.</strong></p>" +
                "<p>If you did not request this, please ignore this email.</p>" +
                "</body></html>",
                verificationLink, verificationLink
            );

            helper.setText(bodyText, bodyHtml);

            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage(), e);
            throw new EmailException("Failed to send verification email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending verification email to {}: {}", to, e.getMessage(), e);
            throw new EmailException("Unexpected error sending verification email: " + e.getMessage(), e);
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

