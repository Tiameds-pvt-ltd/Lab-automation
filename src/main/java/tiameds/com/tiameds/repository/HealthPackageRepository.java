package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.HealthPackage;
import tiameds.com.tiameds.entity.Lab;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Transactional
@Repository
public interface HealthPackageRepository extends JpaRepository<HealthPackage, Long> {

    List<HealthPackage> findByLabs_Id(Long labId);

    boolean existsByPackageName(String packageName);

    List<HealthPackage> findAllByLabs(Lab lab);

    @Query("SELECT COUNT(h) FROM HealthPackage h JOIN h.labs l WHERE l.id = :labId AND h.createdAt BETWEEN :startDate AND :endDate")
    long countByLabId(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value =
        "SELECT hp.package_id AS packageId, hp.package_name AS packageName, hp.package_code AS packageCode, " +
        "COUNT(DISTINCT pvp.visit_id) AS visitCount, " +
        "ROUND(hp.price::numeric * COUNT(DISTINCT pvp.visit_id), 2) AS revenue, " +
        "ROUND(COALESCE(SUM(hp.discount::numeric), 0), 2) AS discount, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "  ELSE hp.price::numeric * COALESCE(b.actual_received_amount, 0) / vps.total_price END), 0)::numeric, 2) AS paidRevenue, " +
        "ROUND(COALESCE(SUM(" +
        "  hp.price::numeric - COALESCE(hp.discount::numeric, 0) " +
        "  - CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "    ELSE hp.price::numeric * COALESCE(b.actual_received_amount, 0) / vps.total_price END" +
        "), 0)::numeric, 2) AS dueRevenue, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "  hp.price::numeric * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.cash_total, 0) " +
        "    WHEN UPPER(b.payment_method) = 'CASH' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END / vps.total_price END), 0)::numeric, 2) AS cashRevenue, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "  hp.price::numeric * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.upi_total, 0) " +
        "    WHEN UPPER(b.payment_method) = 'UPI' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END / vps.total_price END), 0)::numeric, 2) AS upiRevenue, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "  hp.price::numeric * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.card_total, 0) " +
        "    WHEN UPPER(b.payment_method) = 'CARD' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END / vps.total_price END), 0)::numeric, 2) AS cardRevenue " +
        "FROM health_packages hp " +
        "JOIN lab_packages lp ON hp.package_id = lp.package_id " +
        "JOIN labs l ON lp.lab_id = l.lab_id " +
        "LEFT JOIN patient_visit_packages pvp ON pvp.package_id = hp.package_id " +
        "LEFT JOIN patient_visits pv ON pvp.visit_id = pv.visit_id " +
        "LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
        "LEFT JOIN (SELECT bt.billing_id, COALESCE(SUM(bt.cash_amount), 0) AS cash_total, " +
        "  COALESCE(SUM(bt.upi_amount), 0) AS upi_total, COALESCE(SUM(bt.card_amount), 0) AS card_total " +
        "  FROM billing_transaction bt GROUP BY bt.billing_id) bt_agg ON bt_agg.billing_id = b.billing_id " +
        "LEFT JOIN (SELECT pv2.visit_id, " +
        "  COALESCE(ts.test_total, 0) + COALESCE(ps.pkg_total, 0) AS total_price " +
        "  FROM patient_visits pv2 " +
        "  LEFT JOIN (SELECT vtr2.visit_id, SUM(t2.price) AS test_total " +
        "    FROM visit_test_result vtr2 JOIN tests t2 ON vtr2.test_id = t2.test_id " +
        "    GROUP BY vtr2.visit_id) ts ON ts.visit_id = pv2.visit_id " +
        "  LEFT JOIN (SELECT pvp2.visit_id, SUM(hp2.price::numeric) AS pkg_total " +
        "    FROM patient_visit_packages pvp2 JOIN health_packages hp2 ON pvp2.package_id = hp2.package_id " +
        "    GROUP BY pvp2.visit_id) ps ON ps.visit_id = pv2.visit_id " +
        ") vps ON vps.visit_id = pv.visit_id " +
        "WHERE l.created_by = :createdById " +
        "GROUP BY hp.package_id, hp.package_name, hp.package_code, hp.price " +
        "ORDER BY revenue DESC", nativeQuery = true)
    List<PackageSummaryProjection> getPackageSummaryBySuperAdmin(@Param("createdById") Long createdById);

    @Query(value =
        "SELECT hp.package_id AS packageId, hp.package_name AS packageName, hp.package_code AS packageCode, " +
        "COUNT(DISTINCT pvp.visit_id) AS visitCount, " +
        "ROUND(hp.price::numeric * COUNT(DISTINCT pvp.visit_id), 2) AS revenue, " +
        "ROUND(COALESCE(SUM(hp.discount::numeric), 0), 2) AS discount, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "  ELSE hp.price::numeric * COALESCE(b.actual_received_amount, 0) / vps.total_price END), 0)::numeric, 2) AS paidRevenue, " +
        "ROUND(COALESCE(SUM(" +
        "  hp.price::numeric - COALESCE(hp.discount::numeric, 0) " +
        "  - CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "    ELSE hp.price::numeric * COALESCE(b.actual_received_amount, 0) / vps.total_price END" +
        "), 0)::numeric, 2) AS dueRevenue, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "  hp.price::numeric * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.cash_total, 0) " +
        "    WHEN UPPER(b.payment_method) = 'CASH' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END / vps.total_price END), 0)::numeric, 2) AS cashRevenue, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "  hp.price::numeric * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.upi_total, 0) " +
        "    WHEN UPPER(b.payment_method) = 'UPI' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END / vps.total_price END), 0)::numeric, 2) AS upiRevenue, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "  hp.price::numeric * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.card_total, 0) " +
        "    WHEN UPPER(b.payment_method) = 'CARD' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END / vps.total_price END), 0)::numeric, 2) AS cardRevenue " +
        "FROM health_packages hp " +
        "JOIN lab_packages lp ON hp.package_id = lp.package_id " +
        "JOIN labs l ON lp.lab_id = l.lab_id " +
        "JOIN (SELECT pvp_f.package_id, pvp_f.visit_id " +
        "  FROM patient_visit_packages pvp_f " +
        "  JOIN patient_visits pv_f ON pvp_f.visit_id = pv_f.visit_id " +
        "  WHERE pv_f.created_at BETWEEN :startDate AND :endDate) pvp ON pvp.package_id = hp.package_id " +
        "JOIN patient_visits pv ON pvp.visit_id = pv.visit_id " +
        "LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
        "LEFT JOIN (SELECT bt.billing_id, COALESCE(SUM(bt.cash_amount), 0) AS cash_total, " +
        "  COALESCE(SUM(bt.upi_amount), 0) AS upi_total, COALESCE(SUM(bt.card_amount), 0) AS card_total " +
        "  FROM billing_transaction bt GROUP BY bt.billing_id) bt_agg ON bt_agg.billing_id = b.billing_id " +
        "LEFT JOIN (SELECT pv2.visit_id, " +
        "  COALESCE(ts.test_total, 0) + COALESCE(ps.pkg_total, 0) AS total_price " +
        "  FROM patient_visits pv2 " +
        "  LEFT JOIN (SELECT vtr2.visit_id, SUM(t2.price) AS test_total " +
        "    FROM visit_test_result vtr2 JOIN tests t2 ON vtr2.test_id = t2.test_id " +
        "    GROUP BY vtr2.visit_id) ts ON ts.visit_id = pv2.visit_id " +
        "  LEFT JOIN (SELECT pvp2.visit_id, SUM(hp2.price::numeric) AS pkg_total " +
        "    FROM patient_visit_packages pvp2 JOIN health_packages hp2 ON pvp2.package_id = hp2.package_id " +
        "    GROUP BY pvp2.visit_id) ps ON ps.visit_id = pv2.visit_id " +
        ") vps ON vps.visit_id = pv.visit_id " +
        "WHERE l.created_by = :createdById " +
        "GROUP BY hp.package_id, hp.package_name, hp.package_code, hp.price " +
        "ORDER BY revenue DESC", nativeQuery = true)
    List<PackageSummaryProjection> getPackageSummaryBySuperAdminWithDateRange(
            @Param("createdById") Long createdById,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(value =
        "SELECT hp.package_id AS packageId, hp.package_name AS packageName, hp.package_code AS packageCode, " +
        "COUNT(DISTINCT pvp.visit_id) AS visitCount, " +
        "ROUND(hp.price::numeric * COUNT(DISTINCT pvp.visit_id), 2) AS revenue, " +
        "ROUND(COALESCE(SUM(hp.discount::numeric), 0), 2) AS discount, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "  ELSE hp.price::numeric * COALESCE(b.actual_received_amount, 0) / vps.total_price END), 0)::numeric, 2) AS paidRevenue, " +
        "ROUND(COALESCE(SUM(" +
        "  hp.price::numeric - COALESCE(hp.discount::numeric, 0) " +
        "  - CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "    ELSE hp.price::numeric * COALESCE(b.actual_received_amount, 0) / vps.total_price END" +
        "), 0)::numeric, 2) AS dueRevenue, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "  hp.price::numeric * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.cash_total, 0) " +
        "    WHEN UPPER(b.payment_method) = 'CASH' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END / vps.total_price END), 0)::numeric, 2) AS cashRevenue, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "  hp.price::numeric * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.upi_total, 0) " +
        "    WHEN UPPER(b.payment_method) = 'UPI' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END / vps.total_price END), 0)::numeric, 2) AS upiRevenue, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "  hp.price::numeric * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.card_total, 0) " +
        "    WHEN UPPER(b.payment_method) = 'CARD' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END / vps.total_price END), 0)::numeric, 2) AS cardRevenue " +
        "FROM health_packages hp " +
        "JOIN lab_packages lp ON hp.package_id = lp.package_id " +
        "LEFT JOIN patient_visit_packages pvp ON pvp.package_id = hp.package_id " +
        "LEFT JOIN patient_visits pv ON pvp.visit_id = pv.visit_id " +
        "LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
        "LEFT JOIN (SELECT bt.billing_id, COALESCE(SUM(bt.cash_amount), 0) AS cash_total, " +
        "  COALESCE(SUM(bt.upi_amount), 0) AS upi_total, COALESCE(SUM(bt.card_amount), 0) AS card_total " +
        "  FROM billing_transaction bt GROUP BY bt.billing_id) bt_agg ON bt_agg.billing_id = b.billing_id " +
        "LEFT JOIN (SELECT pv2.visit_id, " +
        "  COALESCE(ts.test_total, 0) + COALESCE(ps.pkg_total, 0) AS total_price " +
        "  FROM patient_visits pv2 " +
        "  LEFT JOIN (SELECT vtr2.visit_id, SUM(t2.price) AS test_total " +
        "    FROM visit_test_result vtr2 JOIN tests t2 ON vtr2.test_id = t2.test_id " +
        "    GROUP BY vtr2.visit_id) ts ON ts.visit_id = pv2.visit_id " +
        "  LEFT JOIN (SELECT pvp2.visit_id, SUM(hp2.price::numeric) AS pkg_total " +
        "    FROM patient_visit_packages pvp2 JOIN health_packages hp2 ON pvp2.package_id = hp2.package_id " +
        "    GROUP BY pvp2.visit_id) ps ON ps.visit_id = pv2.visit_id " +
        ") vps ON vps.visit_id = pv.visit_id " +
        "WHERE lp.lab_id = :labId " +
        "GROUP BY hp.package_id, hp.package_name, hp.package_code, hp.price " +
        "ORDER BY revenue DESC", nativeQuery = true)
    List<PackageSummaryProjection> getPackageSummaryByLabId(@Param("labId") Long labId);

    @Query(value =
        "SELECT hp.package_id AS packageId, hp.package_name AS packageName, hp.package_code AS packageCode, " +
        "COUNT(DISTINCT pvp.visit_id) AS visitCount, " +
        "ROUND(hp.price::numeric * COUNT(DISTINCT pvp.visit_id), 2) AS revenue, " +
        "ROUND(COALESCE(SUM(hp.discount::numeric), 0), 2) AS discount, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "  ELSE hp.price::numeric * COALESCE(b.actual_received_amount, 0) / vps.total_price END), 0)::numeric, 2) AS paidRevenue, " +
        "ROUND(COALESCE(SUM(" +
        "  hp.price::numeric - COALESCE(hp.discount::numeric, 0) " +
        "  - CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "    ELSE hp.price::numeric * COALESCE(b.actual_received_amount, 0) / vps.total_price END" +
        "), 0)::numeric, 2) AS dueRevenue, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "  hp.price::numeric * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.cash_total, 0) " +
        "    WHEN UPPER(b.payment_method) = 'CASH' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END / vps.total_price END), 0)::numeric, 2) AS cashRevenue, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "  hp.price::numeric * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.upi_total, 0) " +
        "    WHEN UPPER(b.payment_method) = 'UPI' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END / vps.total_price END), 0)::numeric, 2) AS upiRevenue, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "  hp.price::numeric * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.card_total, 0) " +
        "    WHEN UPPER(b.payment_method) = 'CARD' THEN COALESCE(b.actual_received_amount, 0) ELSE 0 END / vps.total_price END), 0)::numeric, 2) AS cardRevenue " +
        "FROM health_packages hp " +
        "JOIN lab_packages lp ON hp.package_id = lp.package_id " +
        "JOIN (SELECT pvp_f.package_id, pvp_f.visit_id " +
        "  FROM patient_visit_packages pvp_f " +
        "  JOIN patient_visits pv_f ON pvp_f.visit_id = pv_f.visit_id " +
        "  WHERE pv_f.created_at BETWEEN :startDate AND :endDate) pvp ON pvp.package_id = hp.package_id " +
        "JOIN patient_visits pv ON pvp.visit_id = pv.visit_id " +
        "LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
        "LEFT JOIN (SELECT bt.billing_id, COALESCE(SUM(bt.cash_amount), 0) AS cash_total, " +
        "  COALESCE(SUM(bt.upi_amount), 0) AS upi_total, COALESCE(SUM(bt.card_amount), 0) AS card_total " +
        "  FROM billing_transaction bt GROUP BY bt.billing_id) bt_agg ON bt_agg.billing_id = b.billing_id " +
        "LEFT JOIN (SELECT pv2.visit_id, " +
        "  COALESCE(ts.test_total, 0) + COALESCE(ps.pkg_total, 0) AS total_price " +
        "  FROM patient_visits pv2 " +
        "  LEFT JOIN (SELECT vtr2.visit_id, SUM(t2.price) AS test_total " +
        "    FROM visit_test_result vtr2 JOIN tests t2 ON vtr2.test_id = t2.test_id " +
        "    GROUP BY vtr2.visit_id) ts ON ts.visit_id = pv2.visit_id " +
        "  LEFT JOIN (SELECT pvp2.visit_id, SUM(hp2.price::numeric) AS pkg_total " +
        "    FROM patient_visit_packages pvp2 JOIN health_packages hp2 ON pvp2.package_id = hp2.package_id " +
        "    GROUP BY pvp2.visit_id) ps ON ps.visit_id = pv2.visit_id " +
        ") vps ON vps.visit_id = pv.visit_id " +
        "WHERE lp.lab_id = :labId " +
        "GROUP BY hp.package_id, hp.package_name, hp.package_code, hp.price " +
        "ORDER BY revenue DESC", nativeQuery = true)
    List<PackageSummaryProjection> getPackageSummaryByLabIdWithDateRange(
            @Param("labId") Long labId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(value =
        "SELECT hp.package_id AS packageId, hp.package_name AS packageName, hp.package_code AS packageCode, " +
        "COUNT(DISTINCT pvp.visit_id) AS visitCount, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount, 0) IS NULL THEN 0 " +
        "  ELSE hp.price::numeric * COALESCE(b.actual_received_amount, 0) / b.total_amount END), 0), 2) AS revenue, " +
        "ROUND(0, 2) AS discount, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount, 0) IS NULL THEN 0 " +
        "  ELSE hp.price::numeric * COALESCE(b.actual_received_amount, 0) / b.total_amount END), 0), 2) AS paidRevenue, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL THEN hp.price::numeric " +
        "  WHEN NULLIF(b.total_amount, 0) IS NULL THEN hp.price::numeric " +
        "  ELSE hp.price::numeric * COALESCE(b.due_amount, 0) / b.total_amount END), 0), 2) AS dueRevenue, " +
        "ROUND(0, 2) AS cashRevenue, ROUND(0, 2) AS upiRevenue, ROUND(0, 2) AS cardRevenue " +
        "FROM health_packages hp " +
        "JOIN lab_packages lp ON hp.package_id = lp.package_id " +
        "LEFT JOIN patient_visit_packages pvp ON pvp.package_id = hp.package_id " +
        "LEFT JOIN patient_visits pv ON pvp.visit_id = pv.visit_id " +
        "LEFT JOIN lab_visit lv ON pv.visit_id = lv.visit_id AND lv.lab_id = :labId " +
        "LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
        "WHERE lp.lab_id = :labId " +
        "GROUP BY hp.package_id, hp.package_name, hp.package_code " +
        "ORDER BY visitCount DESC " +
        "LIMIT :limit", nativeQuery = true)
    List<PackageSummaryProjection> getPackagePerformanceByLabId(
            @Param("labId") Long labId,
            @Param("limit") int limit);

    @Query(value =
        "SELECT hp.package_id AS packageId, hp.package_name AS packageName, hp.package_code AS packageCode, " +
        "COUNT(DISTINCT pvp.visit_id) AS visitCount, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount, 0) IS NULL THEN 0 " +
        "  ELSE hp.price::numeric * COALESCE(b.actual_received_amount, 0) / b.total_amount END), 0), 2) AS revenue, " +
        "ROUND(0, 2) AS discount, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount, 0) IS NULL THEN 0 " +
        "  ELSE hp.price::numeric * COALESCE(b.actual_received_amount, 0) / b.total_amount END), 0), 2) AS paidRevenue, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL THEN hp.price::numeric " +
        "  WHEN NULLIF(b.total_amount, 0) IS NULL THEN hp.price::numeric " +
        "  ELSE hp.price::numeric * COALESCE(b.due_amount, 0) / b.total_amount END), 0), 2) AS dueRevenue, " +
        "ROUND(0, 2) AS cashRevenue, ROUND(0, 2) AS upiRevenue, ROUND(0, 2) AS cardRevenue " +
        "FROM health_packages hp " +
        "JOIN lab_packages lp ON hp.package_id = lp.package_id " +
        "LEFT JOIN patient_visit_packages pvp ON pvp.package_id = hp.package_id " +
        "LEFT JOIN patient_visits pv ON pvp.visit_id = pv.visit_id AND pv.created_at BETWEEN :startDate AND :endDate " +
        "LEFT JOIN lab_visit lv ON pv.visit_id = lv.visit_id AND lv.lab_id = :labId " +
        "LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
        "WHERE lp.lab_id = :labId " +
        "GROUP BY hp.package_id, hp.package_name, hp.package_code " +
        "ORDER BY visitCount DESC " +
        "LIMIT :limit", nativeQuery = true)
    List<PackageSummaryProjection> getPackagePerformanceByLabIdAndDateRange(
            @Param("labId") Long labId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("limit") int limit);

    interface PackageSummaryProjection {
        Long getPackageId();
        String getPackageName();
        String getPackageCode();
        Long getVisitCount();
        BigDecimal getRevenue();
        BigDecimal getDiscount();
        BigDecimal getPaidRevenue();
        BigDecimal getDueRevenue();
        BigDecimal getCashRevenue();
        BigDecimal getUpiRevenue();
        BigDecimal getCardRevenue();
    }
}
