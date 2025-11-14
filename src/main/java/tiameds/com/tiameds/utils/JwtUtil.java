package tiameds.com.tiameds.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tiameds.com.tiameds.config.JwtProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String CLAIM_TOKEN_VERSION = "tokenVersion";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";

    public record JwtToken(String value, Instant expiresAt) {
    }

    public record RefreshJwtToken(String value, Instant expiresAt, UUID id) {
    }

    private final JwtProperties jwtProperties;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    void initKeys() {
        try {
            this.privateKey = loadPrivateKey();
            this.publicKey = loadPublicKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Unable to load RSA keys for JWT processing", e);
        }
    }

    public JwtToken generateAccessToken(String username, Integer tokenVersion) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiry = now.plus(jwtProperties.getAccessTokenTtl());

        String token = createToken(Map.of(CLAIM_TOKEN_VERSION, tokenVersion, CLAIM_TOKEN_TYPE, TokenType.ACCESS.value),
                username,
                now,
                expiry,
                null);
        return new JwtToken(token, expiry);
    }

    public RefreshJwtToken generateRefreshToken(String username, Integer tokenVersion) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiry = now.plus(jwtProperties.getRefreshTokenTtl());
        UUID tokenId = UUID.randomUUID();

        String token = createToken(Map.of(
                        CLAIM_TOKEN_VERSION, tokenVersion,
                        CLAIM_TOKEN_TYPE, TokenType.REFRESH.value),
                username,
                now,
                expiry,
                tokenId);
        return new RefreshJwtToken(token, expiry, tokenId);
    }

    public String generateToken(String username, Integer tokenVersion) {
        return generateAccessToken(username, tokenVersion).value();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Integer extractTokenVersion(String token) {
        Object value = parseClaims(token).get(CLAIM_TOKEN_VERSION);
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    public UUID extractTokenId(String token) {
        Claims claims = parseClaims(token);
        String jti = claims.getId();
        return jti != null ? UUID.fromString(jti) : null;
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    public Claims parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        ensureTokenType(claims, TokenType.ACCESS);
        ensureNotExpired(claims);
        return claims;
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        ensureTokenType(claims, TokenType.REFRESH);
        ensureNotExpired(claims);
        return claims;
    }

    public boolean validateToken(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .requireIssuer(jwtProperties.getIssuer())
                .requireAudience(jwtProperties.getAudience())
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String createToken(Map<String, Object> claims,
                               String subject,
                               Instant issuedAt,
                               Instant expiresAt,
                               UUID tokenId) {
        var builder = Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(jwtProperties.getIssuer())
                .setAudience(jwtProperties.getAudience())
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .signWith(privateKey, SignatureAlgorithm.RS256);

        if (tokenId != null) {
            builder.setId(tokenId.toString());
        }

        return builder.compact();
    }

    private void ensureTokenType(Claims claims, TokenType expectedType) {
        String claimValue = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (claimValue == null || !expectedType.value.equalsIgnoreCase(claimValue)) {
            throw new JwtException("Unexpected token type");
        }
    }

    private void ensureNotExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        if (expiration == null || expiration.before(new Date())) {
            throw new ExpiredJwtException(null, claims, "Token has expired");
        }
    }

    private PrivateKey loadPrivateKey() throws IOException, GeneralSecurityException {
        byte[] keyBytes = readKey(jwtProperties.getPrivateKeyLocation(), "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private PublicKey loadPublicKey() throws IOException, GeneralSecurityException {
        byte[] keyBytes = readKey(jwtProperties.getPublicKeyLocation(), "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private byte[] readKey(org.springframework.core.io.Resource resource, String prefix, String suffix) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            String key = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            String sanitized = key
                    .replace(prefix, "")
                    .replace(suffix, "")
                    .replaceAll("\\s", "");
            return Base64.getDecoder().decode(sanitized);
        }
    }

    private enum TokenType {
        ACCESS("access"),
        REFRESH("refresh");

        private final String value;

        TokenType(String value) {
            this.value = value;
        }
    }
}

