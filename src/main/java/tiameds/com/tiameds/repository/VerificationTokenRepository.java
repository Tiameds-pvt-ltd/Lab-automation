package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.VerificationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Transactional
@Repository
public interface VerificationTokenRepository extends CrudRepository<VerificationToken, Long> {

    /**
     * Find a token by its identifier (for faster lookup)
     */
    Optional<VerificationToken> findByTokenIdentifier(String tokenIdentifier);

    /**
     * Find a token by its hash (for validation)
     */
    Optional<VerificationToken> findByTokenHash(String tokenHash);

    /**
     * Find all tokens for an email (for rate limiting)
     */
    List<VerificationToken> findByEmailOrderByCreatedAtDesc(String email);

    /**
     * Count tokens created for an email within a time window (for rate limiting)
     * This query counts tokens created in the last X minutes
     */
    @Query("SELECT COUNT(vt) FROM VerificationToken vt WHERE vt.email = :email AND vt.createdAt >= :since")
    long countByEmailAndCreatedAtAfter(@Param("email") String email, @Param("since") Instant since);

    /**
     * Find valid (unused and not expired) token by hash
     */
    @Query("SELECT vt FROM VerificationToken vt WHERE vt.tokenHash = :tokenHash AND vt.used = false AND vt.expiryTime > :now")
    Optional<VerificationToken> findValidTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    /**
     * Mark token as used
     */
    @Modifying
    @Transactional
    @Query("UPDATE VerificationToken vt SET vt.used = true, vt.usedAt = :usedAt WHERE vt.id = :id")
    void markAsUsed(@Param("id") Long id, @Param("usedAt") Instant usedAt);

    /**
     * Clean up expired tokens (optional, for maintenance)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiryTime < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
}

