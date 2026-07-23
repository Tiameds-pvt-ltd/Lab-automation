package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VisitTestResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VisitTestResultRepository extends JpaRepository<VisitTestResult , Long> {

    // Fetch a specific test result for a given visit and test
    @Query("SELECT vtr FROM VisitTestResult vtr WHERE vtr.visit.visitId = :visitId AND vtr.test.id = :testId")
    Optional<VisitTestResult> findByVisitIdAndTestId(Long visitId, Long testId);

    // Total number of tests ordered for a patient across all visits in a lab
    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE vtr.visit.patient.patientId = :patientId AND l.id = :labId")
    long countByPatientIdAndLabId(@Param("patientId") Long patientId, @Param("labId") Long labId);

    // Count tests by reportStatus ("Pending" or "Completed") for a patient in a lab — DB stores title case
    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE vtr.visit.patient.patientId = :patientId AND l.id = :labId AND vtr.reportStatus = :reportStatus")
    long countByPatientIdAndLabIdAndReportStatus(@Param("patientId") Long patientId, @Param("labId") Long labId, @Param("reportStatus") String reportStatus);

    // Count tests that were cancelled (testStatus = CANCELLED) for a patient in a lab
    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE vtr.visit.patient.patientId = :patientId AND l.id = :labId AND vtr.testStatus = 'CANCELLED'")
    long countCancelledByPatientIdAndLabId(@Param("patientId") Long patientId, @Param("labId") Long labId);

    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE vtr.visit.patient.patientId = :patientId AND l.id = :labId AND vtr.createdAt BETWEEN :startDate AND :endDate")
    long countByPatientIdAndLabIdAndCreatedAtBetween(@Param("patientId") Long patientId, @Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE vtr.visit.patient.patientId = :patientId AND l.id = :labId AND vtr.reportStatus = :reportStatus AND vtr.createdAt BETWEEN :startDate AND :endDate")
    long countByPatientIdAndLabIdAndReportStatusAndCreatedAtBetween(@Param("patientId") Long patientId, @Param("labId") Long labId, @Param("reportStatus") String reportStatus, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE vtr.visit.patient.patientId = :patientId AND l.id = :labId AND vtr.testStatus = 'CANCELLED' AND vtr.createdAt BETWEEN :startDate AND :endDate")
    long countCancelledByPatientIdAndLabIdAndCreatedAtBetween(@Param("patientId") Long patientId, @Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Count all patient-ordered tests across all labs created by a superadmin
    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE l.createdBy = :createdBy")
    long countAllTestsByLabsCreatedBy(@Param("createdBy") User createdBy);

    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE l.createdBy = :createdBy AND vtr.createdAt BETWEEN :startDate AND :endDate")
    long countAllTestsByLabsCreatedByAndCreatedAtBetween(@Param("createdBy") User createdBy, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Count completed reports across all labs created by a superadmin
    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE l.createdBy = :createdBy AND vtr.reportStatus = 'Completed'")
    long countCompletedReportsByLabsCreatedBy(@Param("createdBy") User createdBy);

    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE l.createdBy = :createdBy AND vtr.reportStatus = 'Completed' AND vtr.createdAt BETWEEN :startDate AND :endDate")
    long countCompletedReportsByLabsCreatedByAndCreatedAtBetween(@Param("createdBy") User createdBy, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Count all patient-ordered tests for a specific lab
    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE l.id = :labId")
    long countAllTestsByLabId(@Param("labId") Long labId);

    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE l.id = :labId AND vtr.createdAt BETWEEN :startDate AND :endDate")
    long countAllTestsByLabIdAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE l.id = :labId AND vtr.reportStatus = 'Completed'")
    long countCompletedReportsByLabId(@Param("labId") Long labId);

    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE l.id = :labId AND vtr.reportStatus = 'Completed' AND vtr.createdAt BETWEEN :startDate AND :endDate")
    long countCompletedReportsByLabIdAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Funnel — test-level counts (consistent granularity across all stages)

    // Tests in visits that have at least one physical sample collected (Samples Collected stage)
    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId AND SIZE(v.visitSamples) > 0")
    long countTestsInVisitsWithSamplesByLabId(@Param("labId") Long labId);

    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId AND SIZE(v.visitSamples) > 0 AND vtr.createdAt BETWEEN :startDate AND :endDate")
    long countTestsInVisitsWithSamplesByLabIdAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Tests where result has been entered (isFilled = true) — Results Entered stage
    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.patient p JOIN p.labs l WHERE l.id = :labId AND vtr.isFilled = true")
    long countFilledTestsByLabId(@Param("labId") Long labId);

    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.patient p JOIN p.labs l WHERE l.id = :labId AND vtr.isFilled = true AND vtr.createdAt BETWEEN :startDate AND :endDate")
    long countFilledTestsByLabIdAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Tests in visits with Completed status — Reports Delivered stage
    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId AND v.visitStatus = 'Completed'")
    long countTestsInCompletedVisitsByLabId(@Param("labId") Long labId);

    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId AND v.visitStatus = 'Completed' AND vtr.createdAt BETWEEN :startDate AND :endDate")
    long countTestsInCompletedVisitsByLabIdAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Patient-ordered tests grouped by category — super admin scope (all categories shown, 0 if no orders)
    @Query(value = "SELECT cats.category AS category, COALESCE(vtr_agg.testCount, 0) AS testCount, COALESCE(vtr_agg.revenue, 0) AS revenue " +
            "FROM (SELECT DISTINCT t.category FROM tests t JOIN lab_tests lt ON t.test_id = lt.test_id JOIN labs l ON lt.lab_id = l.lab_id WHERE l.created_by = :createdById) cats " +
            "LEFT JOIN (SELECT t.category, COUNT(*) AS testCount, SUM(t.price) AS revenue FROM visit_test_result vtr JOIN patient_visits pv ON vtr.visit_id = pv.visit_id JOIN lab_visit lv ON pv.visit_id = lv.visit_id JOIN labs l ON lv.lab_id = l.lab_id JOIN tests t ON vtr.test_id = t.test_id WHERE l.created_by = :createdById GROUP BY t.category) vtr_agg ON vtr_agg.category = cats.category " +
            "ORDER BY testCount DESC", nativeQuery = true)
    List<TestsByCategoryProjection> getPatientTestsByCategoryBySuperAdmin(@Param("createdById") Long createdById);

    @Query(value = "SELECT cats.category AS category, COALESCE(vtr_agg.testCount, 0) AS testCount, COALESCE(vtr_agg.revenue, 0) AS revenue " +
            "FROM (SELECT DISTINCT t.category FROM tests t JOIN lab_tests lt ON t.test_id = lt.test_id JOIN labs l ON lt.lab_id = l.lab_id WHERE l.created_by = :createdById) cats " +
            "LEFT JOIN (SELECT t.category, COUNT(*) AS testCount, SUM(t.price) AS revenue FROM visit_test_result vtr JOIN patient_visits pv ON vtr.visit_id = pv.visit_id JOIN lab_visit lv ON pv.visit_id = lv.visit_id JOIN labs l ON lv.lab_id = l.lab_id JOIN tests t ON vtr.test_id = t.test_id WHERE l.created_by = :createdById AND vtr.created_at BETWEEN :startDate AND :endDate GROUP BY t.category) vtr_agg ON vtr_agg.category = cats.category " +
            "ORDER BY testCount DESC", nativeQuery = true)
    List<TestsByCategoryProjection> getPatientTestsByCategoryBySuperAdminWithDateRange(@Param("createdById") Long createdById, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Patient-ordered tests grouped by category — lab admin scope (all categories shown, 0 if no orders)
    @Query(value = "SELECT cats.category AS category, COALESCE(vtr_agg.testCount, 0) AS testCount, COALESCE(vtr_agg.revenue, 0) AS revenue " +
            "FROM (SELECT DISTINCT t.category FROM tests t JOIN lab_tests lt ON t.test_id = lt.test_id WHERE lt.lab_id = :labId) cats " +
            "LEFT JOIN (SELECT t.category, COUNT(*) AS testCount, SUM(t.price) AS revenue FROM visit_test_result vtr JOIN lab_visit lv ON vtr.visit_id = lv.visit_id JOIN tests t ON vtr.test_id = t.test_id WHERE lv.lab_id = :labId GROUP BY t.category) vtr_agg ON vtr_agg.category = cats.category " +
            "ORDER BY testCount DESC", nativeQuery = true)
    List<TestsByCategoryProjection> getPatientTestsByCategoryByLabId(@Param("labId") Long labId);

    @Query(value = "SELECT cats.category AS category, COALESCE(vtr_agg.testCount, 0) AS testCount, COALESCE(vtr_agg.revenue, 0) AS revenue " +
            "FROM (SELECT DISTINCT t.category FROM tests t JOIN lab_tests lt ON t.test_id = lt.test_id WHERE lt.lab_id = :labId) cats " +
            "LEFT JOIN (SELECT t.category, COUNT(*) AS testCount, SUM(t.price) AS revenue FROM visit_test_result vtr JOIN lab_visit lv ON vtr.visit_id = lv.visit_id JOIN tests t ON vtr.test_id = t.test_id WHERE lv.lab_id = :labId AND vtr.created_at BETWEEN :startDate AND :endDate GROUP BY t.category) vtr_agg ON vtr_agg.category = cats.category " +
            "ORDER BY testCount DESC", nativeQuery = true)
    List<TestsByCategoryProjection> getPatientTestsByCategoryByLabIdWithDateRange(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (r.created_at - v.created_at)) / 3600.0) FROM lab_report r JOIN patient_visits v ON r.visit_id = v.visit_id WHERE r.lab_id = :labId", nativeQuery = true)
    Double getAvgTatHoursByLabId(@Param("labId") Long labId);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (r.created_at - v.created_at)) / 3600.0) FROM lab_report r JOIN patient_visits v ON r.visit_id = v.visit_id WHERE r.lab_id = :labId AND v.created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    Double getAvgTatHoursByLabIdAndDateRange(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    interface TestsByCategoryProjection {
        String getCategory();
        Long getTestCount();
        BigDecimal getRevenue();
    }

    @Query(value =
        "SELECT cats.category AS category, " +
        "COALESCE(vtr_agg.testCount, 0) AS testCount, " +
        "ROUND(COALESCE(vtr_agg.revenue, 0), 2) AS revenue, " +
        "ROUND(COALESCE(vtr_agg.discount, 0), 2) AS discount, " +
        "ROUND(COALESCE(vtr_agg.paidRevenue, 0), 2) AS paidRevenue, " +
        "ROUND(COALESCE(vtr_agg.dueRevenue, 0), 2) AS dueRevenue, " +
        "ROUND(COALESCE(vtr_agg.cashRevenue, 0), 2) AS cashRevenue, " +
        "ROUND(COALESCE(vtr_agg.upiRevenue, 0), 2) AS upiRevenue, " +
        "ROUND(COALESCE(vtr_agg.cardRevenue, 0), 2) AS cardRevenue " +
        "FROM (SELECT DISTINCT t.category FROM tests t JOIN lab_tests lt ON t.test_id = lt.test_id JOIN labs l ON lt.lab_id = l.lab_id WHERE l.created_by = :createdById) cats " +
        "LEFT JOIN ( " +
        "  SELECT t.category, " +
        "    COUNT(*) AS testCount, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "             ELSE t.price * COALESCE(b.actual_received_amount, 0) / vps.total_price END) AS revenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "             ELSE t.price * COALESCE(b.discount, 0) / vps.total_price END) AS discount, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "             ELSE t.price * COALESCE(b.actual_received_amount, 0) / vps.total_price END) AS paidRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL THEN t.price " +
        "             WHEN NULLIF(vps.total_price, 0) IS NULL THEN t.price " +
        "             ELSE t.price * COALESCE(b.due_amount, 0) / vps.total_price END) AS dueRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "          t.price * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.cash_total, 0) " +
        "                         WHEN UPPER(b.payment_method) = 'CASH' THEN COALESCE(b.actual_received_amount, 0) " +
        "                         ELSE 0 END / vps.total_price END) AS cashRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "          t.price * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.upi_total, 0) " +
        "                         WHEN UPPER(b.payment_method) = 'UPI' THEN COALESCE(b.actual_received_amount, 0) " +
        "                         ELSE 0 END / vps.total_price END) AS upiRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "          t.price * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.card_total, 0) " +
        "                         WHEN UPPER(b.payment_method) = 'CARD' THEN COALESCE(b.actual_received_amount, 0) " +
        "                         ELSE 0 END / vps.total_price END) AS cardRevenue " +
        "  FROM visit_test_result vtr " +
        "  JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
        "  JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
        "  JOIN labs l ON lv.lab_id = l.lab_id " +
        "  JOIN tests t ON vtr.test_id = t.test_id " +
        "  LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
        "  LEFT JOIN ( " +
        "    SELECT bt.billing_id, " +
        "      COALESCE(SUM(bt.cash_amount), 0) AS cash_total, " +
        "      COALESCE(SUM(bt.upi_amount), 0) AS upi_total, " +
        "      COALESCE(SUM(bt.card_amount), 0) AS card_total " +
        "    FROM billing_transaction bt GROUP BY bt.billing_id " +
        "  ) bt_agg ON bt_agg.billing_id = b.billing_id " +
        "  LEFT JOIN (SELECT vtr2.visit_id, SUM(t2.price) AS total_price " +
        "    FROM visit_test_result vtr2 JOIN tests t2 ON vtr2.test_id = t2.test_id " +
        "    GROUP BY vtr2.visit_id) vps ON vps.visit_id = pv.visit_id " +
        "  WHERE l.created_by = :createdById " +
        "  GROUP BY t.category " +
        ") vtr_agg ON vtr_agg.category = cats.category " +
        "ORDER BY testCount DESC", nativeQuery = true)
    List<TestsByCategoryDetailedProjection> getPatientTestsByCategoryDetailedBySuperAdmin(@Param("createdById") Long createdById);

    @Query(value =
        "SELECT cats.category AS category, " +
        "COALESCE(vtr_agg.testCount, 0) AS testCount, " +
        "ROUND(COALESCE(vtr_agg.revenue, 0), 2) AS revenue, " +
        "ROUND(COALESCE(vtr_agg.discount, 0), 2) AS discount, " +
        "ROUND(COALESCE(vtr_agg.paidRevenue, 0), 2) AS paidRevenue, " +
        "ROUND(COALESCE(vtr_agg.dueRevenue, 0), 2) AS dueRevenue, " +
        "ROUND(COALESCE(vtr_agg.cashRevenue, 0), 2) AS cashRevenue, " +
        "ROUND(COALESCE(vtr_agg.upiRevenue, 0), 2) AS upiRevenue, " +
        "ROUND(COALESCE(vtr_agg.cardRevenue, 0), 2) AS cardRevenue " +
        "FROM (SELECT DISTINCT t.category FROM tests t JOIN lab_tests lt ON t.test_id = lt.test_id JOIN labs l ON lt.lab_id = l.lab_id WHERE l.created_by = :createdById) cats " +
        "LEFT JOIN ( " +
        "  SELECT t.category, " +
        "    COUNT(*) AS testCount, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "             ELSE t.price * COALESCE(b.actual_received_amount, 0) / vps.total_price END) AS revenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "             ELSE t.price * COALESCE(b.discount, 0) / vps.total_price END) AS discount, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "             ELSE t.price * COALESCE(b.actual_received_amount, 0) / vps.total_price END) AS paidRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL THEN t.price " +
        "             WHEN NULLIF(vps.total_price, 0) IS NULL THEN t.price " +
        "             ELSE t.price * COALESCE(b.due_amount, 0) / vps.total_price END) AS dueRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "          t.price * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.cash_total, 0) " +
        "                         WHEN UPPER(b.payment_method) = 'CASH' THEN COALESCE(b.actual_received_amount, 0) " +
        "                         ELSE 0 END / vps.total_price END) AS cashRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "          t.price * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.upi_total, 0) " +
        "                         WHEN UPPER(b.payment_method) = 'UPI' THEN COALESCE(b.actual_received_amount, 0) " +
        "                         ELSE 0 END / vps.total_price END) AS upiRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "          t.price * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.card_total, 0) " +
        "                         WHEN UPPER(b.payment_method) = 'CARD' THEN COALESCE(b.actual_received_amount, 0) " +
        "                         ELSE 0 END / vps.total_price END) AS cardRevenue " +
        "  FROM visit_test_result vtr " +
        "  JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
        "  JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
        "  JOIN labs l ON lv.lab_id = l.lab_id " +
        "  JOIN tests t ON vtr.test_id = t.test_id " +
        "  LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
        "  LEFT JOIN ( " +
        "    SELECT bt.billing_id, " +
        "      COALESCE(SUM(bt.cash_amount), 0) AS cash_total, " +
        "      COALESCE(SUM(bt.upi_amount), 0) AS upi_total, " +
        "      COALESCE(SUM(bt.card_amount), 0) AS card_total " +
        "    FROM billing_transaction bt GROUP BY bt.billing_id " +
        "  ) bt_agg ON bt_agg.billing_id = b.billing_id " +
        "  LEFT JOIN (SELECT vtr2.visit_id, SUM(t2.price) AS total_price " +
        "    FROM visit_test_result vtr2 JOIN tests t2 ON vtr2.test_id = t2.test_id " +
        "    GROUP BY vtr2.visit_id) vps ON vps.visit_id = pv.visit_id " +
        "  WHERE l.created_by = :createdById AND vtr.created_at BETWEEN :startDate AND :endDate " +
        "  GROUP BY t.category " +
        ") vtr_agg ON vtr_agg.category = cats.category " +
        "ORDER BY testCount DESC", nativeQuery = true)
    List<TestsByCategoryDetailedProjection> getPatientTestsByCategoryDetailedBySuperAdminWithDateRange(
            @Param("createdById") Long createdById,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    interface TestsByCategoryDetailedProjection {
        String getCategory();
        Long getTestCount();
        BigDecimal getRevenue();
        BigDecimal getDiscount();
        BigDecimal getPaidRevenue();
        BigDecimal getDueRevenue();
        BigDecimal getCashRevenue();
        BigDecimal getUpiRevenue();
        BigDecimal getCardRevenue();
    }

    @Query(value =
        "SELECT t.category AS category, t.test_id AS testId, t.name AS testName, " +
        "t.test_code AS testCode, t.price AS testPrice, COUNT(*) AS orderedCount, " +
        "ROUND(t.price * COUNT(*), 2) AS totalEarnings, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "  ELSE t.price * COALESCE(b.actual_received_amount, 0) / vps.total_price END), 0), 2) AS paidAmount, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL THEN t.price " +
        "  WHEN NULLIF(vps.total_price, 0) IS NULL THEN t.price " +
        "  ELSE t.price * COALESCE(b.due_amount, 0) / vps.total_price END), 0), 2) AS dueAmount " +
        "FROM visit_test_result vtr " +
        "JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
        "JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
        "JOIN labs l ON lv.lab_id = l.lab_id " +
        "JOIN tests t ON vtr.test_id = t.test_id " +
        "LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
        "LEFT JOIN (SELECT vtr2.visit_id, SUM(t2.price) AS total_price " +
        "  FROM visit_test_result vtr2 JOIN tests t2 ON vtr2.test_id = t2.test_id " +
        "  GROUP BY vtr2.visit_id) vps ON vps.visit_id = pv.visit_id " +
        "WHERE l.created_by = :createdById " +
        "GROUP BY t.category, t.test_id, t.name, t.test_code, t.price " +
        "ORDER BY t.category, paidAmount DESC", nativeQuery = true)
    List<TestEarningsByTestProjection> getEarningsByTestBySuperAdmin(@Param("createdById") Long createdById);

    @Query(value =
        "SELECT t.category AS category, t.test_id AS testId, t.name AS testName, " +
        "t.test_code AS testCode, t.price AS testPrice, COUNT(*) AS orderedCount, " +
        "ROUND(t.price * COUNT(*), 2) AS totalEarnings, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "  ELSE t.price * COALESCE(b.actual_received_amount, 0) / vps.total_price END), 0), 2) AS paidAmount, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL THEN t.price " +
        "  WHEN NULLIF(vps.total_price, 0) IS NULL THEN t.price " +
        "  ELSE t.price * COALESCE(b.due_amount, 0) / vps.total_price END), 0), 2) AS dueAmount " +
        "FROM visit_test_result vtr " +
        "JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
        "JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
        "JOIN labs l ON lv.lab_id = l.lab_id " +
        "JOIN tests t ON vtr.test_id = t.test_id " +
        "LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
        "LEFT JOIN (SELECT vtr2.visit_id, SUM(t2.price) AS total_price " +
        "  FROM visit_test_result vtr2 JOIN tests t2 ON vtr2.test_id = t2.test_id " +
        "  GROUP BY vtr2.visit_id) vps ON vps.visit_id = pv.visit_id " +
        "WHERE l.created_by = :createdById AND vtr.created_at BETWEEN :startDate AND :endDate " +
        "GROUP BY t.category, t.test_id, t.name, t.test_code, t.price " +
        "ORDER BY t.category, paidAmount DESC", nativeQuery = true)
    List<TestEarningsByTestProjection> getEarningsByTestBySuperAdminWithDateRange(
            @Param("createdById") Long createdById,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    interface TestEarningsByTestProjection {
        String getCategory();
        Long getTestId();
        String getTestName();
        String getTestCode();
        BigDecimal getTestPrice();
        Long getOrderedCount();
        BigDecimal getTotalEarnings();
        BigDecimal getPaidAmount();
        BigDecimal getDueAmount();
    }

    @Query(value =
        "SELECT t.category AS category, t.test_id AS testId, t.name AS testName, " +
        "t.test_code AS testCode, t.price AS testPrice, COUNT(*) AS orderedCount, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount, 0) IS NULL THEN 0 " +
        "  ELSE t.price * COALESCE(b.actual_received_amount, 0) / b.total_amount END), 0), 2) AS totalEarnings, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount, 0) IS NULL THEN 0 " +
        "  ELSE t.price * COALESCE(b.actual_received_amount, 0) / b.total_amount END), 0), 2) AS paidAmount, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL THEN t.price " +
        "  WHEN NULLIF(b.total_amount, 0) IS NULL THEN t.price " +
        "  ELSE t.price * COALESCE(b.due_amount, 0) / b.total_amount END), 0), 2) AS dueAmount " +
        "FROM visit_test_result vtr " +
        "JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
        "JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
        "JOIN tests t ON vtr.test_id = t.test_id " +
        "LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
        "WHERE lv.lab_id = :labId " +
        "GROUP BY t.category, t.test_id, t.name, t.test_code, t.price " +
        "ORDER BY t.category, totalEarnings DESC", nativeQuery = true)
    List<TestEarningsByTestProjection> getEarningsByTestByLabId(@Param("labId") Long labId);

    @Query(value =
        "SELECT t.category AS category, t.test_id AS testId, t.name AS testName, " +
        "t.test_code AS testCode, t.price AS testPrice, COUNT(*) AS orderedCount, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount, 0) IS NULL THEN 0 " +
        "  ELSE t.price * COALESCE(b.actual_received_amount, 0) / b.total_amount END), 0), 2) AS totalEarnings, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(b.total_amount, 0) IS NULL THEN 0 " +
        "  ELSE t.price * COALESCE(b.actual_received_amount, 0) / b.total_amount END), 0), 2) AS paidAmount, " +
        "ROUND(COALESCE(SUM(CASE WHEN b.billing_id IS NULL THEN t.price " +
        "  WHEN NULLIF(b.total_amount, 0) IS NULL THEN t.price " +
        "  ELSE t.price * COALESCE(b.due_amount, 0) / b.total_amount END), 0), 2) AS dueAmount " +
        "FROM visit_test_result vtr " +
        "JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
        "JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
        "JOIN tests t ON vtr.test_id = t.test_id " +
        "LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
        "WHERE lv.lab_id = :labId AND vtr.created_at BETWEEN :startDate AND :endDate " +
        "GROUP BY t.category, t.test_id, t.name, t.test_code, t.price " +
        "ORDER BY t.category, totalEarnings DESC", nativeQuery = true)
    List<TestEarningsByTestProjection> getEarningsByTestByLabIdWithDateRange(
            @Param("labId") Long labId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value =
        "SELECT cats.category AS category, " +
        "COALESCE(vtr_agg.testCount, 0) AS testCount, " +
        "ROUND(COALESCE(vtr_agg.revenue, 0), 2) AS revenue, " +
        "ROUND(COALESCE(vtr_agg.discount, 0), 2) AS discount, " +
        "ROUND(COALESCE(vtr_agg.paidRevenue, 0), 2) AS paidRevenue, " +
        "ROUND(COALESCE(vtr_agg.dueRevenue, 0), 2) AS dueRevenue, " +
        "ROUND(COALESCE(vtr_agg.cashRevenue, 0), 2) AS cashRevenue, " +
        "ROUND(COALESCE(vtr_agg.upiRevenue, 0), 2) AS upiRevenue, " +
        "ROUND(COALESCE(vtr_agg.cardRevenue, 0), 2) AS cardRevenue " +
        "FROM (SELECT DISTINCT t.category FROM tests t JOIN lab_tests lt ON t.test_id = lt.test_id WHERE lt.lab_id = :labId) cats " +
        "LEFT JOIN ( " +
        "  SELECT t.category, " +
        "    COUNT(*) AS testCount, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "             ELSE t.price * COALESCE(b.actual_received_amount, 0) / vps.total_price END) AS revenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "             ELSE t.price * COALESCE(b.discount, 0) / vps.total_price END) AS discount, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "             ELSE t.price * COALESCE(b.actual_received_amount, 0) / vps.total_price END) AS paidRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL THEN t.price " +
        "             WHEN NULLIF(vps.total_price, 0) IS NULL THEN t.price " +
        "             ELSE t.price * COALESCE(b.due_amount, 0) / vps.total_price END) AS dueRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "          t.price * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.cash_total, 0) " +
        "                         WHEN UPPER(b.payment_method) = 'CASH' THEN COALESCE(b.actual_received_amount, 0) " +
        "                         ELSE 0 END / vps.total_price END) AS cashRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "          t.price * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.upi_total, 0) " +
        "                         WHEN UPPER(b.payment_method) = 'UPI' THEN COALESCE(b.actual_received_amount, 0) " +
        "                         ELSE 0 END / vps.total_price END) AS upiRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "          t.price * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.card_total, 0) " +
        "                         WHEN UPPER(b.payment_method) = 'CARD' THEN COALESCE(b.actual_received_amount, 0) " +
        "                         ELSE 0 END / vps.total_price END) AS cardRevenue " +
        "  FROM visit_test_result vtr " +
        "  JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
        "  JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
        "  JOIN tests t ON vtr.test_id = t.test_id " +
        "  LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
        "  LEFT JOIN ( " +
        "    SELECT bt.billing_id, " +
        "      COALESCE(SUM(bt.cash_amount), 0) AS cash_total, " +
        "      COALESCE(SUM(bt.upi_amount), 0) AS upi_total, " +
        "      COALESCE(SUM(bt.card_amount), 0) AS card_total " +
        "    FROM billing_transaction bt GROUP BY bt.billing_id " +
        "  ) bt_agg ON bt_agg.billing_id = b.billing_id " +
        "  LEFT JOIN (SELECT vtr2.visit_id, SUM(t2.price) AS total_price " +
        "    FROM visit_test_result vtr2 JOIN tests t2 ON vtr2.test_id = t2.test_id " +
        "    GROUP BY vtr2.visit_id) vps ON vps.visit_id = pv.visit_id " +
        "  WHERE lv.lab_id = :labId " +
        "  GROUP BY t.category " +
        ") vtr_agg ON vtr_agg.category = cats.category " +
        "ORDER BY testCount DESC", nativeQuery = true)
    List<TestsByCategoryDetailedProjection> getPatientTestsByCategoryDetailedByLabId(@Param("labId") Long labId);

    @Query(value =
        "SELECT cats.category AS category, " +
        "COALESCE(vtr_agg.testCount, 0) AS testCount, " +
        "ROUND(COALESCE(vtr_agg.revenue, 0), 2) AS revenue, " +
        "ROUND(COALESCE(vtr_agg.discount, 0), 2) AS discount, " +
        "ROUND(COALESCE(vtr_agg.paidRevenue, 0), 2) AS paidRevenue, " +
        "ROUND(COALESCE(vtr_agg.dueRevenue, 0), 2) AS dueRevenue, " +
        "ROUND(COALESCE(vtr_agg.cashRevenue, 0), 2) AS cashRevenue, " +
        "ROUND(COALESCE(vtr_agg.upiRevenue, 0), 2) AS upiRevenue, " +
        "ROUND(COALESCE(vtr_agg.cardRevenue, 0), 2) AS cardRevenue " +
        "FROM (SELECT DISTINCT t.category FROM tests t JOIN lab_tests lt ON t.test_id = lt.test_id WHERE lt.lab_id = :labId) cats " +
        "LEFT JOIN ( " +
        "  SELECT t.category, " +
        "    COUNT(*) AS testCount, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "             ELSE t.price * COALESCE(b.actual_received_amount, 0) / vps.total_price END) AS revenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "             ELSE t.price * COALESCE(b.discount, 0) / vps.total_price END) AS discount, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 " +
        "             ELSE t.price * COALESCE(b.actual_received_amount, 0) / vps.total_price END) AS paidRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL THEN t.price " +
        "             WHEN NULLIF(vps.total_price, 0) IS NULL THEN t.price " +
        "             ELSE t.price * COALESCE(b.due_amount, 0) / vps.total_price END) AS dueRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "          t.price * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.cash_total, 0) " +
        "                         WHEN UPPER(b.payment_method) = 'CASH' THEN COALESCE(b.actual_received_amount, 0) " +
        "                         ELSE 0 END / vps.total_price END) AS cashRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "          t.price * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.upi_total, 0) " +
        "                         WHEN UPPER(b.payment_method) = 'UPI' THEN COALESCE(b.actual_received_amount, 0) " +
        "                         ELSE 0 END / vps.total_price END) AS upiRevenue, " +
        "    SUM(CASE WHEN b.billing_id IS NULL OR NULLIF(vps.total_price, 0) IS NULL THEN 0 ELSE " +
        "          t.price * CASE WHEN bt_agg.billing_id IS NOT NULL THEN COALESCE(bt_agg.card_total, 0) " +
        "                         WHEN UPPER(b.payment_method) = 'CARD' THEN COALESCE(b.actual_received_amount, 0) " +
        "                         ELSE 0 END / vps.total_price END) AS cardRevenue " +
        "  FROM visit_test_result vtr " +
        "  JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
        "  JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
        "  JOIN tests t ON vtr.test_id = t.test_id " +
        "  LEFT JOIN billing b ON pv.billing_id = b.billing_id " +
        "  LEFT JOIN ( " +
        "    SELECT bt.billing_id, " +
        "      COALESCE(SUM(bt.cash_amount), 0) AS cash_total, " +
        "      COALESCE(SUM(bt.upi_amount), 0) AS upi_total, " +
        "      COALESCE(SUM(bt.card_amount), 0) AS card_total " +
        "    FROM billing_transaction bt GROUP BY bt.billing_id " +
        "  ) bt_agg ON bt_agg.billing_id = b.billing_id " +
        "  LEFT JOIN (SELECT vtr2.visit_id, SUM(t2.price) AS total_price " +
        "    FROM visit_test_result vtr2 JOIN tests t2 ON vtr2.test_id = t2.test_id " +
        "    GROUP BY vtr2.visit_id) vps ON vps.visit_id = pv.visit_id " +
        "  WHERE lv.lab_id = :labId AND vtr.created_at BETWEEN :startDate AND :endDate " +
        "  GROUP BY t.category " +
        ") vtr_agg ON vtr_agg.category = cats.category " +
        "ORDER BY testCount DESC", nativeQuery = true)
    List<TestsByCategoryDetailedProjection> getPatientTestsByCategoryDetailedByLabIdWithDateRange(
            @Param("labId") Long labId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Top ordered tests for a specific lab
    @Query(value = "SELECT t.name AS testName, t.test_code AS testCode, COUNT(*) AS orderedCount " +
            "FROM visit_test_result vtr " +
            "JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
            "JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
            "JOIN tests t ON vtr.test_id = t.test_id " +
            "WHERE lv.lab_id = :labId " +
            "GROUP BY t.test_id, t.name, t.test_code " +
            "ORDER BY orderedCount DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<TopOrderedTestProjection> getTopOrderedTestsByLabId(@Param("labId") Long labId, @Param("limit") int limit);

    @Query(value = "SELECT t.name AS testName, t.test_code AS testCode, COUNT(*) AS orderedCount " +
            "FROM visit_test_result vtr " +
            "JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
            "JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
            "JOIN tests t ON vtr.test_id = t.test_id " +
            "WHERE lv.lab_id = :labId AND vtr.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY t.test_id, t.name, t.test_code " +
            "ORDER BY orderedCount DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<TopOrderedTestProjection> getTopOrderedTestsByLabIdAndCreatedAtBetween(
            @Param("labId") Long labId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("limit") int limit);

    interface TopOrderedTestProjection {
        String getTestName();
        String getTestCode();
        Long getOrderedCount();
    }
}
