package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tiameds.com.tiameds.entity.BillingEntity;
import tiameds.com.tiameds.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface BillingRepository extends JpaRepository<BillingEntity, Long> {

    @Query("SELECT COUNT(b) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.paymentStatus = :status AND b.createdAt BETWEEN :startDate AND :endDate")
    long countByLabIdAndStatus(@Param("labId") Long labId, @Param("status") String status, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.paymentStatus = 'PAID'")
    BigDecimal sumTotalRevenueByLabIdAllTime(@Param("labId") Long labId);

    @Query("SELECT SUM(b.totalAmount) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.paymentStatus = 'PAID' AND b.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalByLabId(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(b) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.createdAt BETWEEN :startDate AND :endDate")
    long countByLabId(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT SUM(b.discount) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.discount > 0 AND b.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumDiscountByLabId(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT SUM(b.totalAmount) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.totalAmount > 0 AND b.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumGrossByLabId(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM BillingEntity b JOIN b.labs l WHERE l.createdBy = :createdBy AND b.paymentStatus = 'PAID'")
    BigDecimal sumTotalRevenueByLabsCreatedBy(@Param("createdBy") User createdBy);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM BillingEntity b JOIN b.labs l WHERE l.createdBy = :createdBy AND b.paymentStatus = 'PAID' AND b.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalRevenueByLabsCreatedByAndCreatedAtBetween(@Param("createdBy") User createdBy, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query(value = "SELECT DATE(b.created_at) AS date, COALESCE(SUM(b.total_amount), 0) AS revenue " +
            "FROM billing b " +
            "JOIN lab_billing lb ON b.billing_id = lb.billing_id " +
            "JOIN labs l ON lb.lab_id = l.lab_id " +
            "WHERE l.created_by = :createdById " +
            "AND b.payment_status = 'PAID' " +
            "AND b.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(b.created_at) " +
            "ORDER BY DATE(b.created_at)", nativeQuery = true)
    List<DailyRevenueProjection> getDailyRevenueTrend(@Param("createdById") Long createdById, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    interface DailyRevenueProjection {
        String getDate();
        BigDecimal getRevenue();
    }

    @Query(value = "SELECT l.name AS labName, COALESCE(SUM(b.total_amount), 0) AS revenue " +
            "FROM billing b " +
            "JOIN lab_billing lb ON b.billing_id = lb.billing_id " +
            "JOIN labs l ON lb.lab_id = l.lab_id " +
            "WHERE l.created_by = :createdById " +
            "AND b.payment_status = 'PAID' " +
            "AND b.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY l.lab_id, l.name " +
            "ORDER BY revenue DESC " +
            "LIMIT 8", nativeQuery = true)
    List<RevenueByLabProjection> getRevenueByLab(@Param("createdById") Long createdById, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query(value = "SELECT l.name AS labName, COALESCE(SUM(b.total_amount), 0) AS revenue " +
            "FROM billing b " +
            "JOIN lab_billing lb ON b.billing_id = lb.billing_id " +
            "JOIN labs l ON lb.lab_id = l.lab_id " +
            "WHERE l.created_by = :createdById " +
            "AND b.payment_status = 'PAID' " +
            "GROUP BY l.lab_id, l.name " +
            "ORDER BY revenue DESC " +
            "LIMIT 8", nativeQuery = true)
    List<RevenueByLabProjection> getRevenueByLabAllTime(@Param("createdById") Long createdById);

    @Query(value = "SELECT l.name AS labName, COALESCE(SUM(b.total_amount), 0) AS revenue " +
            "FROM billing b " +
            "JOIN lab_billing lb ON b.billing_id = lb.billing_id " +
            "JOIN labs l ON lb.lab_id = l.lab_id " +
            "WHERE lb.lab_id = :labId " +
            "AND b.payment_status = 'PAID' " +
            "GROUP BY l.lab_id, l.name", nativeQuery = true)
    java.util.Optional<RevenueByLabProjection> getRevenueByLabId(@Param("labId") Long labId);

    @Query(value = "SELECT l.name AS labName, COALESCE(SUM(b.total_amount), 0) AS revenue " +
            "FROM billing b " +
            "JOIN lab_billing lb ON b.billing_id = lb.billing_id " +
            "JOIN labs l ON lb.lab_id = l.lab_id " +
            "WHERE lb.lab_id = :labId " +
            "AND b.payment_status = 'PAID' " +
            "AND b.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY l.lab_id, l.name", nativeQuery = true)
    java.util.Optional<RevenueByLabProjection> getRevenueByLabIdAndDateRange(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query(value = "SELECT DATE(b.created_at) AS date, COALESCE(SUM(b.total_amount), 0) AS revenue " +
            "FROM billing b " +
            "JOIN lab_billing lb ON b.billing_id = lb.billing_id " +
            "WHERE lb.lab_id = :labId " +
            "AND b.payment_status = 'PAID' " +
            "AND b.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(b.created_at) " +
            "ORDER BY DATE(b.created_at)", nativeQuery = true)
    List<DailyRevenueProjection> getDailyRevenueTrendByLabId(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    interface RevenueByLabProjection {
        String getLabName();
        BigDecimal getRevenue();
    }

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
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
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
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            org.springframework.data.domain.Pageable pageable
    );

}
