package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tiameds.com.tiameds.entity.BillingEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface BillingRepository extends JpaRepository<BillingEntity, Long> {

    @Query("SELECT COUNT(b) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.paymentStatus = :status AND b.createdAt BETWEEN :startDate AND :endDate")
    long countByLabIdAndStatus(@Param("labId") Long labId, @Param("status") String status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(b.totalAmount) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalByLabId(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(b) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.createdAt BETWEEN :startDate AND :endDate")
    long countByLabId(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(b.discount) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.discount > 0 AND b.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumDiscountByLabId(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(b.totalAmount) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.totalAmount > 0 AND b.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumGrossByLabId(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);


}
