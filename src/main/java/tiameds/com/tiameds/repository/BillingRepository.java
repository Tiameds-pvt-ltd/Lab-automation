package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tiameds.com.tiameds.entity.BillingEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Query(value = "SELECT DISTINCT b FROM BillingEntity b " +
            "JOIN b.labs l " +
            "JOIN b.visit v " +
            "JOIN v.patient p " +
            "WHERE l.id = :labId " +
            "AND b.createdAt BETWEEN :startDate AND :endDate",
            countQuery = "SELECT COUNT(DISTINCT b) FROM BillingEntity b " +
                    "JOIN b.labs l " +
                    "JOIN b.visit v " +
                    "WHERE l.id = :labId " +
                    "AND b.createdAt BETWEEN :startDate AND :endDate")
    org.springframework.data.domain.Page<BillingEntity> findBillingsByLabAndDateRange(
            @Param("labId") Long labId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * Finds billings by payment date (past bills paid on filter date):
     * - Includes ONLY billings created BEFORE the filter date
     * - AND have transactions where transaction.createdAt is in the date range (payment made on filter date)
     * - Excludes bills created on the filter date
     * - Excludes bills without transactions
     * Note: Sorting by most recent transaction date should be handled in service layer
     */
    @Query(value = "SELECT DISTINCT b FROM BillingEntity b " +
            "JOIN b.labs l " +
            "JOIN b.visit v " +
            "JOIN v.patient p " +
            "WHERE l.id = :labId " +
            "AND b.createdAt < :startDate " +
            "AND EXISTS (SELECT 1 FROM TransactionEntity t WHERE t.billing = b AND t.createdAt BETWEEN :startDate AND :endDate)",
            countQuery = "SELECT COUNT(DISTINCT b) FROM BillingEntity b " +
                    "JOIN b.labs l " +
                    "JOIN b.visit v " +
                    "WHERE l.id = :labId " +
                    "AND b.createdAt < :startDate " +
                    "AND EXISTS (SELECT 1 FROM TransactionEntity t WHERE t.billing = b AND t.createdAt BETWEEN :startDate AND :endDate)")
    org.springframework.data.domain.Page<BillingEntity> findBillingsByLabAndPaymentDateRange(
            @Param("labId") Long labId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            org.springframework.data.domain.Pageable pageable
    );

}
