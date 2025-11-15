package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.PasswordResetToken;
import tiameds.com.tiameds.entity.User;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find a valid (not used, not expired) token by its hash
     * Uses JOIN FETCH to eagerly load the User to avoid LazyInitializationException
     */
    @Query("SELECT prt FROM PasswordResetToken prt " +
           "JOIN FETCH prt.user " +
           "WHERE prt.tokenHash = :tokenHash " +
           "AND prt.used = false " +
           "AND prt.expiresAt > :now")
    Optional<PasswordResetToken> findValidTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    /**
     * Find token by hash (regardless of validity)
     * Uses JOIN FETCH to eagerly load the User to avoid LazyInitializationException
     */
    @Query("SELECT prt FROM PasswordResetToken prt " +
           "JOIN FETCH prt.user " +
           "WHERE prt.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Mark all tokens for a user as used
     */
    @Modifying
    @Transactional
    @Query("UPDATE PasswordResetToken prt SET prt.used = true WHERE prt.user = :user AND prt.used = false")
    void invalidateAllUserTokens(@Param("user") User user);

    /**
     * Delete expired tokens (cleanup)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
}



