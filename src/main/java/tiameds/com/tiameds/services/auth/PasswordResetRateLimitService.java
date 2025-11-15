package tiameds.com.tiameds.services.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.PasswordResetRateLimit;
import tiameds.com.tiameds.repository.PasswordResetRateLimitRepository;

import java.time.Instant;
import java.util.Optional;

/**
 * Database-backed rate limiting service for password reset endpoints
 * Implements rate limiting per email and per IP address
 * 
 * This solution works across multiple application instances by storing
 * rate limit data in PostgreSQL database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetRateLimitService {

    private final PasswordResetRateLimitRepository rateLimitRepository;

    @Value("${password.reset.rate.limit.email.max:3}")
    private int maxRequestsPerEmail;

    @Value("${password.reset.rate.limit.email.window.minutes:1}")
    private int emailWindowMinutes;

    @Value("${password.reset.rate.limit.ip.max:3}")
    private int maxRequestsPerIp;

    @Value("${password.reset.rate.limit.ip.window.minutes:1}")
    private int ipWindowMinutes;

    private static final String EMAIL_PREFIX = "email:";
    private static final String IP_PREFIX = "ip:";

    /**
     * Checks if email is allowed to make a password reset request
     * Returns true if allowed, false if rate limited
     */
    @Transactional
    public boolean isEmailAllowed(String email) {
        if (email == null || email.trim().isEmpty()) {
            return true;
        }

        String normalizedEmail = email.toLowerCase().trim();
        String rateLimitKey = EMAIL_PREFIX + normalizedEmail;
        
        return checkRateLimit(rateLimitKey, maxRequestsPerEmail, emailWindowMinutes);
    }

    /**
     * Checks if IP address is allowed to make a password reset request
     */
    @Transactional
    public boolean isIpAllowed(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return true;
        }

        String rateLimitKey = IP_PREFIX + ipAddress;
        
        return checkRateLimit(rateLimitKey, maxRequestsPerIp, ipWindowMinutes);
    }

    /**
     * Checks if both email and IP are allowed
     */
    @Transactional
    public boolean isAllowed(String email, String ipAddress) {
        return isEmailAllowed(email) && isIpAllowed(ipAddress);
    }

    /**
     * Gets remaining attempts for email (for informational purposes)
     */
    public int getRemainingEmailAttempts(String email) {
        if (email == null || email.trim().isEmpty()) {
            return maxRequestsPerEmail;
        }

        String normalizedEmail = email.toLowerCase().trim();
        String rateLimitKey = EMAIL_PREFIX + normalizedEmail;
        Instant now = Instant.now();

        Optional<PasswordResetRateLimit> rateLimitOpt = rateLimitRepository.findActiveByKey(rateLimitKey, now);
        
        if (rateLimitOpt.isEmpty()) {
            return maxRequestsPerEmail;
        }

        PasswordResetRateLimit rateLimit = rateLimitOpt.get();
        return Math.max(0, maxRequestsPerEmail - rateLimit.getRequestCount());
    }

    /**
     * Core rate limiting logic using database
     */
    private boolean checkRateLimit(String rateLimitKey, int maxRequests, int windowMinutes) {
        Instant now = Instant.now();
        
        // Find existing active rate limit record
        Optional<PasswordResetRateLimit> existingOpt = rateLimitRepository.findActiveByKey(rateLimitKey, now);
        
        if (existingOpt.isPresent()) {
            PasswordResetRateLimit existing = existingOpt.get();
            
            // Check if limit exceeded
            if (existing.getRequestCount() >= maxRequests) {
                log.warn("Password reset rate limit exceeded for key: {} (count: {}/{})", 
                        rateLimitKey, existing.getRequestCount(), maxRequests);
                return false;
            }
            
            // Increment count
            existing.setRequestCount(existing.getRequestCount() + 1);
            rateLimitRepository.save(existing);
            return true;
        } else {
            // Create new rate limit record
            Instant windowStart = now;
            Instant expiresAt = now.plusSeconds(windowMinutes * 60L);
            
            PasswordResetRateLimit newRateLimit = PasswordResetRateLimit.builder()
                    .rateLimitKey(rateLimitKey)
                    .requestCount(1)
                    .windowStart(windowStart)
                    .expiresAt(expiresAt)
                    .createdAt(now)
                    .build();
            
            rateLimitRepository.save(newRateLimit);
            return true;
        }
    }

    /**
     * Scheduled cleanup of expired rate limit records
     * Runs every 5 minutes to clean up old records
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void cleanupExpiredRateLimits() {
        try {
            Instant now = Instant.now();
            rateLimitRepository.deleteExpired(now);
            log.debug("Cleaned up expired password reset rate limit records");
        } catch (Exception e) {
            log.error("Error cleaning up expired rate limits: {}", e.getMessage(), e);
        }
    }

    /**
     * Resets the rate limit for a specific email (for testing/admin purposes)
     */
    @Transactional
    public void resetEmailLimit(String email) {
        if (email != null) {
            String normalizedEmail = email.toLowerCase().trim();
            String rateLimitKey = EMAIL_PREFIX + normalizedEmail;
            rateLimitRepository.deleteByKey(rateLimitKey);
            log.debug("Reset rate limit for email: {}", email);
        }
    }

    /**
     * Resets the rate limit for a specific IP (for testing/admin purposes)
     */
    @Transactional
    public void resetIpLimit(String ipAddress) {
        if (ipAddress != null) {
            String rateLimitKey = IP_PREFIX + ipAddress;
            rateLimitRepository.deleteByKey(rateLimitKey);
            log.debug("Reset rate limit for IP: {}", ipAddress);
        }
    }
}
