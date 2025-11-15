package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.Otp;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    /**
     * Find the most recent unused OTP for an email
     */
    Optional<Otp> findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    /**
     * Count OTP requests for an email within a time window
     */
    @Query("SELECT COUNT(o) FROM Otp o WHERE o.email = :email AND o.createdAt >= :since")
    long countByEmailAndCreatedAtAfter(@Param("email") String email, @Param("since") Instant since);

    /**
     * Find all expired OTPs (for cleanup)
     */
    List<Otp> findByExpiresAtBefore(Instant now);
}

