package tiameds.com.tiameds.services.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.Otp;
import tiameds.com.tiameds.repository.OtpRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_OTP_REQUESTS_PER_WINDOW = 3;
    private static final int OTP_REQUEST_WINDOW_MINUTES = 5;
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;

    private final OtpRepository otpRepository;
    private final SecureRandom random = new SecureRandom();

    /**
     * Checks if the email has exceeded the rate limit for OTP requests
     */
    public boolean isRateLimited(String email) {
        Instant windowStart = Instant.now().minus(OTP_REQUEST_WINDOW_MINUTES, ChronoUnit.MINUTES);
        long requestCount = otpRepository.countByEmailAndCreatedAtAfter(email, windowStart);
        
        boolean rateLimited = requestCount >= MAX_OTP_REQUESTS_PER_WINDOW;
        if (rateLimited) {
            log.warn("Rate limit exceeded for email: {}. Requests in last {} minutes: {}", 
                email, OTP_REQUEST_WINDOW_MINUTES, requestCount);
        }
        return rateLimited;
    }

    /**
     * Generates a random 4-digit OTP
     */
    public String generateOtp() {
        int otp = 1000 + random.nextInt(9000); // Generates 1000-9999
        return String.valueOf(otp);
    }

    /**
     * Saves the OTP to the database after hashing it
     */
    @Transactional
    public void saveOtp(String email, String otp) {
        String otpHash = hashOtp(otp);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(OTP_EXPIRY_MINUTES, ChronoUnit.MINUTES);

        Otp otpEntity = Otp.builder()
                .email(email.toLowerCase().trim())
                .otpHash(otpHash)
                .createdAt(now)
                .expiresAt(expiresAt)
                .attempts(0)
                .used(false)
                .build();

        otpRepository.save(otpEntity);
        log.debug("OTP saved for email: {}", email);
    }

    /**
     * Validates the provided OTP for the given email
     * @throws OtpException if validation fails
     */
    @Transactional
    public void validateOtp(String email, String otp) {
        String normalizedEmail = email.toLowerCase().trim();
        
        // Find the most recent unused OTP for this email
        Otp otpEntity = otpRepository.findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new OtpException("No valid OTP found for this email"));

        // Check if OTP has expired
        if (Instant.now().isAfter(otpEntity.getExpiresAt())) {
            throw new OtpException("OTP has expired. Please request a new one.");
        }

        // Check if maximum attempts exceeded
        if (otpEntity.getAttempts() >= MAX_VERIFICATION_ATTEMPTS) {
            throw new OtpException("Maximum verification attempts exceeded. Please request a new OTP.");
        }

        // Check if already used
        if (otpEntity.getUsed()) {
            throw new OtpException("OTP has already been used. Please request a new one.");
        }

        // Increment attempt counter
        otpEntity.setAttempts(otpEntity.getAttempts() + 1);

        // Verify OTP hash
        String providedOtpHash = hashOtp(otp);
        if (!otpEntity.getOtpHash().equals(providedOtpHash)) {
            otpRepository.save(otpEntity); // Save incremented attempts
            throw new OtpException("Invalid OTP. Please check and try again.");
        }

        // Mark as used
        otpEntity.setUsed(true);
        otpRepository.save(otpEntity);
        log.info("OTP validated successfully for email: {}", normalizedEmail);
    }

    /**
     * Hashes the OTP using SHA-256
     */
    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            
            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("OTP hashing failed", e);
        }
    }

    /**
     * Custom exception for OTP-related errors
     */
    public static class OtpException extends RuntimeException {
        public OtpException(String message) {
            super(message);
        }
    }
}

