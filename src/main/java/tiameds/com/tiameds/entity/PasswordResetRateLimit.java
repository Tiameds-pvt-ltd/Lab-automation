package tiameds.com.tiameds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity for tracking password reset rate limits in database
 * Allows shared rate limiting across multiple application instances
 */
@Entity
@Table(name = "password_reset_rate_limits", indexes = {
    @Index(name = "idx_pwd_reset_rate_limit_key", columnList = "rate_limit_key"),
    @Index(name = "idx_pwd_reset_rate_limit_expires", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRateLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_limit_key", nullable = false, length = 255)
    private String rateLimitKey; // "email:user@example.com" or "ip:127.0.0.1"

    @Column(name = "request_count", nullable = false)
    @Builder.Default
    private Integer requestCount = 1;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}



