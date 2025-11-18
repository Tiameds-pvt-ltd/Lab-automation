package tiameds.com.tiameds.services.onboarding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.VerificationToken;
import tiameds.com.tiameds.repository.VerificationTokenRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Service for managing verification tokens.
 * Handles secure token generation, hashing, validation, and single-use enforcement.
 */
@Slf4j
@Service
public class VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    
    @Value("${onboarding.token.expiry-minutes:15}")
    private int tokenExpiryMinutes;

    @Value("${onboarding.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    public VerificationTokenService(
            VerificationTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generates a secure random token and stores its hash in the database.
     * Returns the plain token (to be sent via email) - it will never be stored in plain form.
     * Uses a two-part token: identifier (for lookup) + verification code (for validation).
     *
     * @param email The email address for which the token is generated
     * @return The plain token string (to be sent to user)
     */
    @Transactional
    public String generateAndStoreToken(String email) {
        // Generate identifier (first 16 chars) for fast lookup
        byte[] identifierBytes = new byte[12];
        secureRandom.nextBytes(identifierBytes);
        String tokenIdentifier = Base64.getUrlEncoder().withoutPadding().encodeToString(identifierBytes).substring(0, 16);

        // Generate verification code (remaining part)
        byte[] verificationBytes = new byte[20];
        secureRandom.nextBytes(verificationBytes);
        String verificationCode = Base64.getUrlEncoder().withoutPadding().encodeToString(verificationBytes);

        // Combine to form full token
        String plainToken = tokenIdentifier + verificationCode;

        // Hash the full token before storing (like a password)
        String tokenHash = passwordEncoder.encode(plainToken);

        // Calculate expiry time (default 15 minutes, configurable)
        Instant expiryTime = Instant.now().plusSeconds(tokenExpiryMinutes * 60L);

        // Create and save the token entity
        VerificationToken token = VerificationToken.builder()
                .email(email)
                .tokenHash(tokenHash)
                .tokenIdentifier(tokenIdentifier)
                .expiryTime(expiryTime)
                .used(false)
                .build();

        tokenRepository.save(token);
        log.info("Generated verification token for email: {} (expires in {} minutes)", email, tokenExpiryMinutes);

        // Return the plain token (only time it exists in plain form)
        return plainToken;
    }

    /**
     * Validates a token and marks it as used if valid.
     * This method is transactional to ensure atomicity of validation and consumption.
     *
     * @param plainToken The plain token from the email link
     * @return Optional containing the VerificationToken if valid, empty otherwise
     */
    @Transactional
    public Optional<VerificationToken> validateAndConsumeToken(String plainToken) {
        if (plainToken == null || plainToken.trim().isEmpty() || plainToken.length() < 16) {
            log.warn("Attempted to validate empty or invalid token");
            return Optional.empty();
        }

        // Extract identifier (first 16 chars)
        String tokenIdentifier = plainToken.substring(0, 16);
        
        // Find token by identifier for fast lookup
        Optional<VerificationToken> tokenOpt = tokenRepository.findByTokenIdentifier(tokenIdentifier);
        
        if (tokenOpt.isEmpty()) {
            log.warn("Token not found for identifier");
            return Optional.empty();
        }

        VerificationToken token = tokenOpt.get();
        Instant now = Instant.now();

        // Check if already used
        if (token.getUsed()) {
            log.warn("Token already used for email: {}", token.getEmail());
            return Optional.empty();
        }

        // Check if expired
        if (token.getExpiryTime().isBefore(now)) {
            log.warn("Token expired for email: {}", token.getEmail());
            return Optional.empty();
        }

        // Verify the full token matches the hash
        if (!passwordEncoder.matches(plainToken, token.getTokenHash())) {
            log.warn("Token hash mismatch for email: {}", token.getEmail());
            return Optional.empty();
        }

        // Token is valid - mark as used atomically
        token.setUsed(true);
        token.setUsedAt(now);
        tokenRepository.save(token);

        log.info("Token validated and consumed for email: {}", token.getEmail());
        return Optional.of(token);
    }

    /**
     * Validates a token without consuming it (for checking validity before showing form).
     * This is useful for the verification endpoint that redirects to the onboarding form.
     *
     * @param plainToken The plain token from the email link
     * @return Optional containing the VerificationToken if valid, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<VerificationToken> validateToken(String plainToken) {
        if (plainToken == null || plainToken.trim().isEmpty() || plainToken.length() < 16) {
            return Optional.empty();
        }

        // Extract identifier (first 16 chars)
        String tokenIdentifier = plainToken.substring(0, 16);
        
        // Find token by identifier
        Optional<VerificationToken> tokenOpt = tokenRepository.findByTokenIdentifier(tokenIdentifier);
        
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        VerificationToken token = tokenOpt.get();
        Instant now = Instant.now();

        // Check if already used
        if (token.getUsed()) {
            return Optional.empty();
        }

        // Check if expired
        if (token.getExpiryTime().isBefore(now)) {
            return Optional.empty();
        }

        // Verify the full token matches the hash
        if (!passwordEncoder.matches(plainToken, token.getTokenHash())) {
            return Optional.empty();
        }

        return Optional.of(token);
    }

    /**
     * Gets the frontend onboarding URL with the token
     */
    public String getOnboardingUrl(String plainToken) {
        String encodedToken = java.net.URLEncoder.encode(plainToken, java.nio.charset.StandardCharsets.UTF_8);
        return frontendBaseUrl + "/onboarding?token=" + encodedToken;
    }
}

