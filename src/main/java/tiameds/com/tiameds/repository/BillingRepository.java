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

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId")
    BigDecimal sumTotalRevenueByLabIdAllTime(@Param("labId") Long labId);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalByLabId(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(b) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.createdAt BETWEEN :startDate AND :endDate")
    long countByLabId(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT SUM(b.discount) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.discount > 0 AND b.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumDiscountByLabId(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT SUM(b.totalAmount) FROM BillingEntity b JOIN b.labs l WHERE l.id = :labId AND b.totalAmount > 0 AND b.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumGrossByLabId(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM BillingEntity b JOIN b.labs l WHERE l.createdBy = :createdBy")
    BigDecimal sumTotalRevenueByLabsCreatedBy(@Param("createdBy") User createdBy);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM BillingEntity b JOIN b.labs l WHERE l.createdBy = :createdBy AND b.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalRevenueByLabsCreatedByAndCreatedAtBetween(@Param("createdBy") User createdBy, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query(value = "SELECT DATE(b.created_at) AS date, COALESCE(SUM(b.total_amount), 0) AS revenue " +
            "FROM billing b " +
            "JOIN lab_billing lb ON b.billing_id = lb.billing_id " +
            "JOIN labs l ON lb.lab_id = l.lab_id " +
            "WHERE l.created_by = :createdById " +
            "AND b.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(b.created_at) " +
            "ORDER BY DATE(b.created_at)", nativeQuery = true)
    List<DailyRevenueProjection> getDailyRevenueTrend(@Param("createdById") Long createdById, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    interface DailyRevenueProjection {
        String getDate();
        BigDecimal getRevenue();
    }

    @Query(value = "SELECT l.name AS labName, " +
            "COALESCE(COALESCE(t_agg.revenue,0) + COALESCE(p_agg.revenue,0),0) AS revenue, " +
            "COALESCE(COALESCE(t_agg.discount,0) + COALESCE(p_agg.discount,0),0) AS discount, " +
            "COALESCE(p_agg.revenue,0) AS packageRevenue " +
            "FROM labs l " +
            "LEFT JOIN ( " +
            "  SELECT lv.lab_id, SUM(t.price) AS revenue, " +
            "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount,0) IS NULL THEN 0 ELSE t.price * COALESCE(b.discount,0)/b.total_amount END) AS discount " +
            "  FROM visit_test_result vtr " +
            "  JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
            "  JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
            "  JOIN tests t ON vtr.test_id = t.test_id " +
            "  LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
            "  WHERE vtr.created_at BETWEEN :startDate AND :endDate " +
            "  GROUP BY lv.lab_id " +
            ") t_agg ON t_agg.lab_id = l.lab_id " +
            "LEFT JOIN ( " +
            "  SELECT lp.lab_id, SUM(hp.price::numeric) AS revenue, " +
            "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount,0) IS NULL THEN 0 ELSE hp.price::numeric * COALESCE(b.discount,0)/b.total_amount END) AS discount " +
            "  FROM lab_packages lp " +
            "  JOIN health_packages hp ON hp.package_id = lp.package_id " +
            "  LEFT JOIN patient_visit_packages pvp ON pvp.package_id = hp.package_id " +
            "  LEFT JOIN patient_visits pv ON pvp.visit_id = pv.visit_id AND pv.created_at BETWEEN :startDate AND :endDate " +
            "  LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
            "  GROUP BY lp.lab_id " +
            ") p_agg ON p_agg.lab_id = l.lab_id " +
            "WHERE l.created_by = :createdById " +
            "ORDER BY revenue DESC " +
            "LIMIT 8", nativeQuery = true)
    List<RevenueByLabProjection> getRevenueByLab(@Param("createdById") Long createdById, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query(value = "SELECT l.name AS labName, " +
            "COALESCE(COALESCE(t_agg.revenue,0) + COALESCE(p_agg.revenue,0),0) AS revenue, " +
            "COALESCE(COALESCE(t_agg.discount,0) + COALESCE(p_agg.discount,0),0) AS discount, " +
            "COALESCE(p_agg.revenue,0) AS packageRevenue " +
            "FROM labs l " +
            "LEFT JOIN ( " +
            "  SELECT lv.lab_id, SUM(t.price) AS revenue, " +
            "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount,0) IS NULL THEN 0 ELSE t.price * COALESCE(b.discount,0)/b.total_amount END) AS discount " +
            "  FROM visit_test_result vtr " +
            "  JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
            "  JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
            "  JOIN tests t ON vtr.test_id = t.test_id " +
            "  LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
            "  GROUP BY lv.lab_id " +
            ") t_agg ON t_agg.lab_id = l.lab_id " +
            "LEFT JOIN ( " +
            "  SELECT lp.lab_id, SUM(hp.price::numeric) AS revenue, " +
            "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount,0) IS NULL THEN 0 ELSE hp.price::numeric * COALESCE(b.discount,0)/b.total_amount END) AS discount " +
            "  FROM lab_packages lp " +
            "  JOIN health_packages hp ON hp.package_id = lp.package_id " +
            "  LEFT JOIN patient_visit_packages pvp ON pvp.package_id = hp.package_id " +
            "  LEFT JOIN patient_visits pv ON pv.visit_id = pvp.visit_id " +
            "  LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
            "  GROUP BY lp.lab_id " +
            ") p_agg ON p_agg.lab_id = l.lab_id " +
            "WHERE l.created_by = :createdById " +
            "ORDER BY revenue DESC " +
            "LIMIT 8", nativeQuery = true)
    List<RevenueByLabProjection> getRevenueByLabAllTime(@Param("createdById") Long createdById);

    @Query(value = "SELECT l.name AS labName, " +
            "COALESCE(COALESCE(t_agg.revenue,0) + COALESCE(p_agg.revenue,0),0) AS revenue, " +
            "COALESCE(COALESCE(t_agg.discount,0) + COALESCE(p_agg.discount,0),0) AS discount, " +
            "COALESCE(p_agg.revenue,0) AS packageRevenue " +
            "FROM labs l " +
            "JOIN lab_billing lb ON lb.lab_id = l.lab_id " +
            "LEFT JOIN ( " +
            "  SELECT lv.lab_id, SUM(t.price) AS revenue, " +
            "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount,0) IS NULL THEN 0 ELSE t.price * COALESCE(b.discount,0)/b.total_amount END) AS discount " +
            "  FROM visit_test_result vtr " +
            "  JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
            "  JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
            "  JOIN tests t ON vtr.test_id = t.test_id " +
            "  LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
            "  GROUP BY lv.lab_id " +
            ") t_agg ON t_agg.lab_id = l.lab_id " +
            "LEFT JOIN ( " +
            "  SELECT lp.lab_id, SUM(hp.price::numeric) AS revenue, " +
            "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount,0) IS NULL THEN 0 ELSE hp.price::numeric * COALESCE(b.discount,0)/b.total_amount END) AS discount " +
            "  FROM lab_packages lp " +
            "  JOIN health_packages hp ON hp.package_id = lp.package_id " +
            "  LEFT JOIN patient_visit_packages pvp ON pvp.package_id = hp.package_id " +
            "  LEFT JOIN patient_visits pv ON pv.visit_id = pvp.visit_id " +
            "  LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
            "  GROUP BY lp.lab_id " +
            ") p_agg ON p_agg.lab_id = l.lab_id " +
            "WHERE lb.lab_id = :labId " +
            "GROUP BY l.lab_id, l.name", nativeQuery = true)
    java.util.Optional<RevenueByLabProjection> getRevenueByLabId(@Param("labId") Long labId);

    @Query(value = "SELECT l.name AS labName, " +
            "COALESCE(COALESCE(t_agg.revenue,0) + COALESCE(p_agg.revenue,0),0) AS revenue, " +
            "COALESCE(COALESCE(t_agg.discount,0) + COALESCE(p_agg.discount,0),0) AS discount, " +
            "COALESCE(p_agg.revenue,0) AS packageRevenue " +
            "FROM labs l " +
            "JOIN lab_billing lb ON lb.lab_id = l.lab_id " +
            "LEFT JOIN ( " +
            "  SELECT lv.lab_id, SUM(t.price) AS revenue, " +
            "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount,0) IS NULL THEN 0 ELSE t.price * COALESCE(b.discount,0)/b.total_amount END) AS discount " +
            "  FROM visit_test_result vtr " +
            "  JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
            "  JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
            "  JOIN tests t ON vtr.test_id = t.test_id " +
            "  LEFT JOIN billing b ON pv.billing_id = b.billing_id AND b.created_at BETWEEN :startDate AND :endDate " +
            "  WHERE vtr.created_at BETWEEN :startDate AND :endDate " +
            "  GROUP BY lv.lab_id " +
            ") t_agg ON t_agg.lab_id = l.lab_id " +
            "LEFT JOIN ( " +
            "  SELECT lp.lab_id, SUM(hp.price::numeric) AS revenue, " +
            "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount,0) IS NULL THEN 0 ELSE hp.price::numeric * COALESCE(b.discount,0)/b.total_amount END) AS discount " +
            "  FROM lab_packages lp " +
            "  JOIN health_packages hp ON hp.package_id = lp.package_id " +
            "  LEFT JOIN patient_visit_packages pvp ON pvp.package_id = hp.package_id " +
            "  LEFT JOIN patient_visits pv ON pvp.visit_id = pv.visit_id AND pv.created_at BETWEEN :startDate AND :endDate " +
            "  LEFT JOIN billing b ON pv.billing_id = b.billing_id AND b.created_at BETWEEN :startDate AND :endDate " +
            "  GROUP BY lp.lab_id " +
            ") p_agg ON p_agg.lab_id = l.lab_id " +
            "WHERE lb.lab_id = :labId " +
            "GROUP BY l.lab_id, l.name", nativeQuery = true)
    java.util.Optional<RevenueByLabProjection> getRevenueByLabIdAndDateRange(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query(value = "SELECT DATE(b.created_at) AS date, COALESCE(SUM(b.total_amount), 0) AS revenue " +
            "FROM billing b " +
            "JOIN lab_billing lb ON b.billing_id = lb.billing_id " +
            "WHERE lb.lab_id = :labId " +
            "AND b.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(b.created_at) " +
            "ORDER BY DATE(b.created_at)", nativeQuery = true)
    List<DailyRevenueProjection> getDailyRevenueTrendByLabId(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    interface RevenueByLabProjection {
        String getLabName();
        BigDecimal getRevenue();
        BigDecimal getDiscount();
        BigDecimal getPackageRevenue();
    }

    @Query(value =
        "SELECT COUNT(DISTINCT b.billing_id) AS totalBillings, " +
        "ROUND(COALESCE(SUM(b.total_amount), 0), 2) AS grossRevenue, " +
        "ROUND(COALESCE(SUM(b.discount), 0), 2) AS totalDiscount, " +
        "ROUND(COALESCE(SUM(b.gst_amount), 0), 2) AS totalGst, " +
        "ROUND(COALESCE(SUM(b.net_amount), 0), 2) AS netRevenue, " +
        "ROUND(COALESCE(SUM(b.actual_received_amount), 0), 2) AS totalPaid, " +
        "ROUND(COALESCE(SUM(b.due_amount), 0), 2) AS totalDue, " +
        "ROUND(COALESCE(SUM(CASE WHEN bt_agg.billing_id IS NOT NULL THEN bt_agg.cash_total " +
        "  WHEN UPPER(b.payment_method) = 'CASH' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END), 0), 2) AS totalCash, " +
        "ROUND(COALESCE(SUM(CASE WHEN bt_agg.billing_id IS NOT NULL THEN bt_agg.upi_total " +
        "  WHEN UPPER(b.payment_method) = 'UPI' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END), 0), 2) AS totalUpi, " +
        "ROUND(COALESCE(SUM(CASE WHEN bt_agg.billing_id IS NOT NULL THEN bt_agg.card_total " +
        "  WHEN UPPER(b.payment_method) = 'CARD' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END), 0), 2) AS totalCard " +
        "FROM billing b " +
        "JOIN lab_billing lb ON b.billing_id = lb.billing_id " +
        "JOIN labs l ON lb.lab_id = l.lab_id " +
        "LEFT JOIN (SELECT bt.billing_id, COALESCE(SUM(bt.cash_amount), 0) AS cash_total, " +
        "  COALESCE(SUM(bt.upi_amount), 0) AS upi_total, COALESCE(SUM(bt.card_amount), 0) AS card_total " +
        "  FROM billing_transaction bt GROUP BY bt.billing_id) bt_agg ON bt_agg.billing_id = b.billing_id " +
        "WHERE l.created_by = :createdById", nativeQuery = true)
    List<DetailedBillingSummaryProjection> getDetailedBillingSummary(@Param("createdById") Long createdById);

    @Query(value =
        "SELECT COUNT(DISTINCT b.billing_id) AS totalBillings, " +
        "ROUND(COALESCE(SUM(b.total_amount), 0), 2) AS grossRevenue, " +
        "ROUND(COALESCE(SUM(b.discount), 0), 2) AS totalDiscount, " +
        "ROUND(COALESCE(SUM(b.gst_amount), 0), 2) AS totalGst, " +
        "ROUND(COALESCE(SUM(b.net_amount), 0), 2) AS netRevenue, " +
        "ROUND(COALESCE(SUM(b.actual_received_amount), 0), 2) AS totalPaid, " +
        "ROUND(COALESCE(SUM(b.due_amount), 0), 2) AS totalDue, " +
        "ROUND(COALESCE(SUM(CASE WHEN bt_agg.billing_id IS NOT NULL THEN bt_agg.cash_total " +
        "  WHEN UPPER(b.payment_method) = 'CASH' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END), 0), 2) AS totalCash, " +
        "ROUND(COALESCE(SUM(CASE WHEN bt_agg.billing_id IS NOT NULL THEN bt_agg.upi_total " +
        "  WHEN UPPER(b.payment_method) = 'UPI' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END), 0), 2) AS totalUpi, " +
        "ROUND(COALESCE(SUM(CASE WHEN bt_agg.billing_id IS NOT NULL THEN bt_agg.card_total " +
        "  WHEN UPPER(b.payment_method) = 'CARD' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END), 0), 2) AS totalCard " +
        "FROM billing b " +
        "JOIN lab_billing lb ON b.billing_id = lb.billing_id " +
        "JOIN labs l ON lb.lab_id = l.lab_id " +
        "LEFT JOIN (SELECT bt.billing_id, COALESCE(SUM(bt.cash_amount), 0) AS cash_total, " +
        "  COALESCE(SUM(bt.upi_amount), 0) AS upi_total, COALESCE(SUM(bt.card_amount), 0) AS card_total " +
        "  FROM billing_transaction bt GROUP BY bt.billing_id) bt_agg ON bt_agg.billing_id = b.billing_id " +
        "WHERE l.created_by = :createdById AND b.created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<DetailedBillingSummaryProjection> getDetailedBillingSummaryWithDateRange(
            @Param("createdById") Long createdById,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    interface DetailedBillingSummaryProjection {
        Long getTotalBillings();
        BigDecimal getGrossRevenue();
        BigDecimal getTotalDiscount();
        BigDecimal getTotalGst();
        BigDecimal getNetRevenue();
        BigDecimal getTotalPaid();
        BigDecimal getTotalDue();
        BigDecimal getTotalCash();
        BigDecimal getTotalUpi();
        BigDecimal getTotalCard();
    }

    @Query(value =
        "SELECT b.payment_status AS status, COUNT(b.billing_id) AS billingCount, " +
        "ROUND(COALESCE(SUM(b.total_amount), 0), 2) AS grossRevenue, " +
        "ROUND(COALESCE(SUM(b.discount), 0), 2) AS totalDiscount, " +
        "ROUND(COALESCE(SUM(b.gst_amount), 0), 2) AS totalGst, " +
        "ROUND(COALESCE(SUM(b.net_amount), 0), 2) AS netRevenue, " +
        "ROUND(COALESCE(SUM(b.actual_received_amount), 0), 2) AS totalPaid, " +
        "ROUND(COALESCE(SUM(b.due_amount), 0), 2) AS totalDue " +
        "FROM billing b " +
        "JOIN lab_billing lb ON b.billing_id = lb.billing_id " +
        "JOIN labs l ON lb.lab_id = l.lab_id " +
        "WHERE l.created_by = :createdById " +
        "GROUP BY b.payment_status " +
        "ORDER BY billingCount DESC", nativeQuery = true)
    List<BillingByStatusProjection> getBillingByStatus(@Param("createdById") Long createdById);

    @Query(value =
        "SELECT b.payment_status AS status, COUNT(b.billing_id) AS billingCount, " +
        "ROUND(COALESCE(SUM(b.total_amount), 0), 2) AS grossRevenue, " +
        "ROUND(COALESCE(SUM(b.discount), 0), 2) AS totalDiscount, " +
        "ROUND(COALESCE(SUM(b.gst_amount), 0), 2) AS totalGst, " +
        "ROUND(COALESCE(SUM(b.net_amount), 0), 2) AS netRevenue, " +
        "ROUND(COALESCE(SUM(b.actual_received_amount), 0), 2) AS totalPaid, " +
        "ROUND(COALESCE(SUM(b.due_amount), 0), 2) AS totalDue " +
        "FROM billing b " +
        "JOIN lab_billing lb ON b.billing_id = lb.billing_id " +
        "JOIN labs l ON lb.lab_id = l.lab_id " +
        "WHERE l.created_by = :createdById AND b.created_at BETWEEN :startDate AND :endDate " +
        "GROUP BY b.payment_status " +
        "ORDER BY billingCount DESC", nativeQuery = true)
    List<BillingByStatusProjection> getBillingByStatusWithDateRange(
            @Param("createdById") Long createdById,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    interface BillingByStatusProjection {
        String getStatus();
        Long getBillingCount();
        BigDecimal getGrossRevenue();
        BigDecimal getTotalDiscount();
        BigDecimal getTotalGst();
        BigDecimal getNetRevenue();
        BigDecimal getTotalPaid();
        BigDecimal getTotalDue();
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
