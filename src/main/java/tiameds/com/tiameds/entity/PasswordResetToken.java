package tiameds.com.tiameds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens", indexes = {
    @Index(name = "idx_password_reset_user_id", columnList = "user_id"),
    @Index(name = "idx_password_reset_token_hash", columnList = "token_hash"),
    @Index(name = "idx_password_reset_expiry", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}



