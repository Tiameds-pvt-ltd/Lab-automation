package tiameds.com.tiameds.services.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.RefreshToken;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.RefreshTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final HexFormat HEX_FORMATTER = HexFormat.of();

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken saveRefreshToken(User user, UUID tokenId, String rawToken, Instant expiresAt) {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(tokenId)
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken validateToken(UUID tokenId, String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findById(tokenId)
                .orElseThrow(() -> new RefreshTokenException(HttpStatus.UNAUTHORIZED, "Refresh token not found"));

        if (refreshToken.isRevoked()) {
            throw new RefreshTokenException(HttpStatus.FORBIDDEN, "Refresh token has been revoked");
        }
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }
        if (!refreshToken.getTokenHash().equals(hashToken(rawToken))) {
            throw new RefreshTokenException(HttpStatus.UNAUTHORIZED, "Refresh token integrity check failed");
        }
        return refreshToken;
    }

    @Transactional
    public void revokeToken(RefreshToken refreshToken) {
        if (!refreshToken.isRevoked()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        }
    }

    @Transactional
    public void revokeToken(UUID tokenId) {
        refreshTokenRepository.findById(tokenId).ifPresent(this::revokeToken);
    }

    @Transactional
    public void revokeAllActiveTokens(Long userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId);
        for (RefreshToken token : activeTokens) {
            token.setRevoked(true);
        }
        refreshTokenRepository.saveAll(activeTokens);
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HEX_FORMATTER.formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash refresh token", e);
        }
    }
}

























