package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.PasswordResetRateLimit;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetRateLimitRepository extends JpaRepository<PasswordResetRateLimit, Long> {

    /**
     * Find active rate limit record by key
     */
    @Query("SELECT prrl FROM PasswordResetRateLimit prrl " +
           "WHERE prrl.rateLimitKey = :key " +
           "AND prrl.expiresAt > :now")
    Optional<PasswordResetRateLimit> findActiveByKey(@Param("key") String key, @Param("now") Instant now);

    /**
     * Delete expired rate limit records (cleanup)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetRateLimit prrl WHERE prrl.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);

    /**
     * Delete all records for a specific key (for reset/cleanup)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetRateLimit prrl WHERE prrl.rateLimitKey = :key")
    void deleteByKey(@Param("key") String key);
}



