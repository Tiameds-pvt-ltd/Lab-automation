package tiameds.com.tiameds.services.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.PasswordResetToken;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.PasswordResetTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;

    @Value("${password.reset.token.expiry.minutes:15}")
    private int tokenExpiryMinutes;

    @Value("${password.reset.token.length:32}")
    private int tokenLengthBytes;

    /**
     * Generates a secure random token for password reset
     * Token is base64 encoded for URL-safe transmission
     */
    public String generateToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[tokenLengthBytes];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Hashes a token using SHA-256 before storing in database
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hexadecimal string
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
            throw new RuntimeException("Token hashing failed", e);
        }
    }

    /**
     * Creates and saves a password reset token for a user
     * Invalidates any existing unused tokens for the user
     */
    @Transactional
    public String createPasswordResetToken(User user) {
        // Invalidate existing unused tokens for this user
        tokenRepository.invalidateAllUserTokens(user);

        // Generate new token
        String plainToken = generateToken();
        String tokenHash = hashToken(plainToken);

        // Calculate expiry time
        Instant expiresAt = Instant.now().plusSeconds(tokenExpiryMinutes * 60L);

        // Save token
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        tokenRepository.save(token);
        log.info("Created password reset token for user: {}", user.getEmail());

        // Return plain token (will be hashed when validated)
        return plainToken;
    }

    /**
     * Validates a password reset token
     * Returns the token entity if valid, throws exception otherwise
     */
    @Transactional
    public PasswordResetToken validateToken(String plainToken) {
        String tokenHash = hashToken(plainToken);
        Instant now = Instant.now();

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findValidTokenByHash(tokenHash, now);

        if (tokenOpt.isEmpty()) {
            // Check if token exists but is expired or used
            Optional<PasswordResetToken> anyToken = tokenRepository.findByTokenHash(tokenHash);
            if (anyToken.isPresent()) {
                PasswordResetToken token = anyToken.get();
                if (token.getUsed()) {
                    throw new PasswordResetException("Token has already been used");
                }
                if (token.getExpiresAt().isBefore(now)) {
                    throw new PasswordResetException("Token has expired");
                }
            }
            throw new PasswordResetException("Invalid or expired token");
        }

        return tokenOpt.get();
    }

    /**
     * Marks a token as used
     */
    @Transactional
    public void markTokenAsUsed(PasswordResetToken token) {
        token.setUsed(true);
        tokenRepository.save(token);
        log.info("Marked password reset token as used for user: {}", token.getUser().getEmail());
    }

    /**
     * Custom exception for password reset errors
     */
    public static class PasswordResetException extends RuntimeException {
        public PasswordResetException(String message) {
            super(message);
        }
    }
}

