package tiameds.com.tiameds.services.onboarding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.repository.VerificationTokenRepository;

import java.time.Instant;

/**
 * Service for database-based rate limiting of email sending.
 * Enforces limits without using in-memory cache, making it suitable for multi-instance deployments.
 */
@Slf4j
@Service
public class EmailRateLimitService {

    private final VerificationTokenRepository tokenRepository;

    @Value("${onboarding.rate-limit.max-emails-per-hour:3}")
    private int maxEmailsPerHour;

    @Value("${onboarding.rate-limit.window-minutes:60}")
    private int windowMinutes;

    public EmailRateLimitService(VerificationTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Checks if an email address can receive another verification email.
     * Uses database queries to count emails sent in the last hour.
     *
     * @param email The email address to check
     * @return true if email can be sent, false if rate limit exceeded
     */
    @Transactional(readOnly = true)
    public boolean canSendEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Calculate the time window (e.g., last 60 minutes)
        Instant since = Instant.now().minusSeconds(windowMinutes * 60L);

        // Count tokens created for this email in the time window
        long emailCount = tokenRepository.countByEmailAndCreatedAtAfter(email, since);

        boolean allowed = emailCount < maxEmailsPerHour;
        
        if (!allowed) {
            log.warn("Rate limit exceeded for email: {} ({} emails in last {} minutes, limit: {})", 
                    email, emailCount, windowMinutes, maxEmailsPerHour);
        } else {
            log.debug("Rate limit check passed for email: {} ({} emails in last {} minutes)", 
                    email, emailCount, windowMinutes);
        }

        return allowed;
    }

    /**
     * Gets the number of emails sent to an address in the current window
     *
     * @param email The email address
     * @return The count of emails sent in the window
     */
    @Transactional(readOnly = true)
    public long getEmailCountInWindow(String email) {
        if (email == null || email.trim().isEmpty()) {
            return 0;
        }

        Instant since = Instant.now().minusSeconds(windowMinutes * 60L);
        return tokenRepository.countByEmailAndCreatedAtAfter(email, since);
    }

    /**
     * Gets the remaining emails allowed for an address in the current window
     *
     * @param email The email address
     * @return The number of emails that can still be sent
     */
    @Transactional(readOnly = true)
    public long getRemainingEmails(String email) {
        long sent = getEmailCountInWindow(email);
        long remaining = maxEmailsPerHour - sent;
        return Math.max(0, remaining);
    }

    /**
     * Gets the time window in minutes
     */
    public int getWindowMinutes() {
        return windowMinutes;
    }

    /**
     * Gets the maximum emails allowed per window
     */
    public int getMaxEmailsPerHour() {
        return maxEmailsPerHour;
    }
}

