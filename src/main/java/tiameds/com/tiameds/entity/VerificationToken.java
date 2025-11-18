package tiameds.com.tiameds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Entity for storing email verification tokens used in the onboarding flow.
 * Tokens are hashed before storage and are single-use only.
 */
@Entity
@Table(name = "verification_tokens", indexes = {
    @Index(name = "idx_verification_token_email", columnList = "email"),
    @Index(name = "idx_verification_token_hash", columnList = "token_hash"),
    @Index(name = "idx_verification_token_identifier", columnList = "token_identifier"),
    @Index(name = "idx_verification_token_expiry", columnList = "expiry_time"),
    @Index(name = "idx_verification_token_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /**
     * Token identifier for faster lookup (first part of token before hashing)
     * This allows us to quickly find candidate tokens without checking all tokens
     */
    @Column(name = "token_identifier", nullable = false, length = 16, unique = true)
    private String tokenIdentifier;

    @Column(name = "expiry_time", nullable = false)
    private Instant expiryTime;

    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "used_at")
    private Instant usedAt;
}

