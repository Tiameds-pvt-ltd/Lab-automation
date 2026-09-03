package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Repository
public interface LabRepository extends JpaRepository<Lab, Long> {

    boolean existsByName(String name);

    @Query("SELECT l FROM Lab l WHERE l.createdBy = :currentUser AND l.isActive = true")
    List<Lab> findByCreatedBy(@Param("currentUser") User currentUser);

    @Query("SELECT l FROM Lab l JOIN FETCH l.members WHERE l.id = :id")
    Optional<Lab> findLabWithMembers(@Param("id") long id);

    Optional<Lab> findById(Long id);


    Optional<Lab> findByMembers(User user);

    @Transactional
    @Query("SELECT l FROM Lab l JOIN l.members m WHERE m.id = :userId")
    Set<Lab> findLabsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(l) FROM Lab l WHERE l.createdBy = :createdBy AND l.isActive = true")
    long countByCreatedBy(@Param("createdBy") User createdBy);

    @Query("SELECT COUNT(l) FROM Lab l WHERE l.createdBy = :createdBy AND l.isActive = true AND l.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedByAndCreatedAtBetween(@Param("createdBy") User createdBy, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value =
        "SELECT l.lab_id AS labId, l.name AS labName, " +
        "COALESCE(curr.revenue, 0) AS revenue, " +
        "COALESCE(prev.revenue, 0) AS previousRevenue, " +
        "COALESCE(vstats.testCount, 0) AS testCount, " +
        "COALESCE(vstats.patientCount, 0) AS patientCount, " +
        "COALESCE(vstats.pendingSamples, 0) AS pendingSamples, " +
        "COALESCE(vstats.reportsGenerated, 0) AS reportsGenerated, " +
        "ROUND(COALESCE(vstats.avgTatHours, 0.0), 1) AS avgTatHours " +
        "FROM labs l " +
        "LEFT JOIN ( " +
        "    SELECT lb.lab_id, SUM(b.actual_received_amount::numeric) AS revenue" +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    JOIN patient_visits pv ON pv.billing_id = b.billing_id " +
        "    WHERE b.created_at BETWEEN :startDate AND :endDate AND LOWER(pv.visit_status) != 'cancelled' " +
        "    GROUP BY lb.lab_id " +
        ") curr ON curr.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lb.lab_id, SUM(b.actual_received_amount::numeric) AS revenue" +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    JOIN patient_visits pv ON pv.billing_id = b.billing_id " +
        "    WHERE b.created_at BETWEEN :prevStartDate AND :prevEndDate AND LOWER(pv.visit_status) != 'cancelled' " +
        "    GROUP BY lb.lab_id " +
        ") prev ON prev.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lv.lab_id, " +
        "        COUNT(DISTINCT vtr.id) AS testCount, " +
        "        COUNT(DISTINCT v.patient_id) AS patientCount, " +
        "        COUNT(DISTINCT CASE WHEN v.visit_status = 'Pending' THEN v.visit_id END) AS pendingSamples, " +
        "        COUNT(DISTINCT CASE WHEN vtr.report_status = 'Completed' THEN vtr.id END) AS reportsGenerated, " +
        "        AVG(CASE WHEN r.report_id IS NOT NULL THEN EXTRACT(EPOCH FROM (r.created_at::timestamptz - v.created_at::timestamptz)) / 3600.0 END) AS avgTatHours " +
        "    FROM lab_visit lv " +
        "    JOIN patient_visits v ON v.visit_id = lv.visit_id " +
        "    LEFT JOIN visit_test_result vtr ON vtr.visit_id = v.visit_id AND LOWER(vtr.test_status) = 'active' " +
        "    LEFT JOIN lab_report r ON r.visit_id = v.visit_id AND r.lab_id = lv.lab_id " +
        "    WHERE v.created_at BETWEEN :startDate AND :endDate AND LOWER(v.visit_status) != 'cancelled' " +
        "    GROUP BY lv.lab_id " +
        ") vstats ON vstats.lab_id = l.lab_id " +
        "WHERE l.created_by = :createdById " +
        "ORDER BY revenue DESC " +
        "LIMIT :limit", nativeQuery = true)
    List<LabPerformanceSummaryProjection> getLabPerformanceSummary(
            @Param("createdById") Long createdById,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("prevStartDate") Instant prevStartDate,
            @Param("prevEndDate") Instant prevEndDate,
            @Param("limit") int limit);

    @Query(value =
        "SELECT l.lab_id AS labId, l.name AS labName, " +
        "COALESCE(curr.revenue, 0) AS revenue, " +
        "0 AS previousRevenue, " +
        "COALESCE(vstats.testCount, 0) AS testCount, " +
        "COALESCE(vstats.patientCount, 0) AS patientCount, " +
        "COALESCE(vstats.pendingSamples, 0) AS pendingSamples, " +
        "COALESCE(vstats.reportsGenerated, 0) AS reportsGenerated, " +
        "ROUND(COALESCE(vstats.avgTatHours, 0.0), 1) AS avgTatHours " +
        "FROM labs l " +
        "LEFT JOIN ( " +
        "    SELECT lb.lab_id, SUM(b.actual_received_amount::numeric) AS revenue" +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    JOIN patient_visits pv ON pv.billing_id = b.billing_id " +
        "    WHERE LOWER(pv.visit_status) != 'cancelled' " +
        "    GROUP BY lb.lab_id " +
        ") curr ON curr.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lv.lab_id, " +
        "        COUNT(DISTINCT vtr.id) AS testCount, " +
        "        COUNT(DISTINCT v.patient_id) AS patientCount, " +
        "        COUNT(DISTINCT CASE WHEN v.visit_status = 'Pending' THEN v.visit_id END) AS pendingSamples, " +
        "        COUNT(DISTINCT CASE WHEN vtr.report_status = 'Completed' THEN vtr.id END) AS reportsGenerated, " +
        "        AVG(CASE WHEN r.report_id IS NOT NULL THEN EXTRACT(EPOCH FROM (r.created_at::timestamptz - v.created_at::timestamptz)) / 3600.0 END) AS avgTatHours " +
        "    FROM lab_visit lv " +
        "    JOIN patient_visits v ON v.visit_id = lv.visit_id " +
        "    LEFT JOIN visit_test_result vtr ON vtr.visit_id = v.visit_id AND LOWER(vtr.test_status) = 'active' " +
        "    LEFT JOIN lab_report r ON r.visit_id = v.visit_id AND r.lab_id = lv.lab_id " +
        "    WHERE LOWER(v.visit_status) != 'cancelled' " +
        "    GROUP BY lv.lab_id " +
        ") vstats ON vstats.lab_id = l.lab_id " +
        "WHERE l.created_by = :createdById " +
        "ORDER BY revenue DESC " +
        "LIMIT :limit", nativeQuery = true)
    List<LabPerformanceSummaryProjection> getLabPerformanceSummaryAllTime(
            @Param("createdById") Long createdById,
            @Param("limit") int limit);

    @Query(value =
        "SELECT l.lab_id AS labId, l.name AS labName, " +
        "COALESCE(curr.revenue, 0) AS revenue, " +
        "0 AS previousRevenue, " +
        "COALESCE(vstats.testCount, 0) AS testCount, " +
        "COALESCE(vstats.patientCount, 0) AS patientCount, " +
        "COALESCE(vstats.pendingSamples, 0) AS pendingSamples, " +
        "COALESCE(vstats.reportsGenerated, 0) AS reportsGenerated, " +
        "ROUND(COALESCE(vstats.avgTatHours, 0.0), 1) AS avgTatHours " +
        "FROM labs l " +
        "LEFT JOIN ( " +
        "    SELECT lb.lab_id, SUM(b.actual_received_amount::numeric) AS revenue" +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    JOIN patient_visits pv ON pv.billing_id = b.billing_id " +
        "    WHERE LOWER(pv.visit_status) != 'cancelled' " +
        "    GROUP BY lb.lab_id " +
        ") curr ON curr.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lv.lab_id, " +
        "        COUNT(DISTINCT vtr.id) AS testCount, " +
        "        COUNT(DISTINCT v.patient_id) AS patientCount, " +
        "        COUNT(DISTINCT CASE WHEN v.visit_status = 'Pending' THEN v.visit_id END) AS pendingSamples, " +
        "        COUNT(DISTINCT CASE WHEN vtr.report_status = 'Completed' THEN vtr.id END) AS reportsGenerated, " +
        "        AVG(CASE WHEN r.report_id IS NOT NULL THEN EXTRACT(EPOCH FROM (r.created_at::timestamptz - v.created_at::timestamptz)) / 3600.0 END) AS avgTatHours " +
        "    FROM lab_visit lv " +
        "    JOIN patient_visits v ON v.visit_id = lv.visit_id " +
        "    LEFT JOIN visit_test_result vtr ON vtr.visit_id = v.visit_id AND LOWER(vtr.test_status) = 'active' " +
        "    LEFT JOIN lab_report r ON r.visit_id = v.visit_id AND r.lab_id = lv.lab_id " +
        "    WHERE LOWER(v.visit_status) != 'cancelled' " +
        "    GROUP BY lv.lab_id " +
        ") vstats ON vstats.lab_id = l.lab_id " +
        "WHERE l.created_by = :createdById " +
        "ORDER BY revenue DESC", nativeQuery = true)
    List<LabPerformanceSummaryProjection> getAllLabsSummaryAllTime(@Param("createdById") Long createdById);

    @Query(value =
        "SELECT l.lab_id AS labId, l.name AS labName, " +
        "COALESCE(curr.revenue, 0) AS revenue, " +
        "COALESCE(prev.revenue, 0) AS previousRevenue, " +
        "COALESCE(vstats.testCount, 0) AS testCount, " +
        "COALESCE(vstats.patientCount, 0) AS patientCount, " +
        "COALESCE(vstats.pendingSamples, 0) AS pendingSamples, " +
        "COALESCE(vstats.reportsGenerated, 0) AS reportsGenerated, " +
        "ROUND(COALESCE(vstats.avgTatHours, 0.0), 1) AS avgTatHours " +
        "FROM labs l " +
        "LEFT JOIN ( " +
        "    SELECT lb.lab_id, SUM(b.actual_received_amount::numeric) AS revenue" +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    JOIN patient_visits pv ON pv.billing_id = b.billing_id " +
        "    WHERE b.created_at BETWEEN :startDate AND :endDate AND LOWER(pv.visit_status) != 'cancelled' " +
        "    GROUP BY lb.lab_id " +
        ") curr ON curr.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lb.lab_id, SUM(b.actual_received_amount::numeric) AS revenue" +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    JOIN patient_visits pv ON pv.billing_id = b.billing_id " +
        "    WHERE b.created_at BETWEEN :prevStartDate AND :prevEndDate AND LOWER(pv.visit_status) != 'cancelled' " +
        "    GROUP BY lb.lab_id " +
        ") prev ON prev.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lv.lab_id, " +
        "        COUNT(DISTINCT vtr.id) AS testCount, " +
        "        COUNT(DISTINCT v.patient_id) AS patientCount, " +
        "        COUNT(DISTINCT CASE WHEN v.visit_status = 'Pending' THEN v.visit_id END) AS pendingSamples, " +
        "        COUNT(DISTINCT CASE WHEN vtr.report_status = 'Completed' THEN vtr.id END) AS reportsGenerated, " +
        "        AVG(CASE WHEN r.report_id IS NOT NULL THEN EXTRACT(EPOCH FROM (r.created_at::timestamptz - v.created_at::timestamptz)) / 3600.0 END) AS avgTatHours " +
        "    FROM lab_visit lv " +
        "    JOIN patient_visits v ON v.visit_id = lv.visit_id " +
        "    LEFT JOIN visit_test_result vtr ON vtr.visit_id = v.visit_id AND LOWER(vtr.test_status) = 'active' " +
        "    LEFT JOIN lab_report r ON r.visit_id = v.visit_id AND r.lab_id = lv.lab_id " +
        "    WHERE v.created_at BETWEEN :startDate AND :endDate AND LOWER(v.visit_status) != 'cancelled' " +
        "    GROUP BY lv.lab_id " +
        ") vstats ON vstats.lab_id = l.lab_id " +
        "WHERE l.created_by = :createdById " +
        "ORDER BY revenue DESC", nativeQuery = true)
    List<LabPerformanceSummaryProjection> getAllLabsSummaryWithDateRange(
            @Param("createdById") Long createdById,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("prevStartDate") Instant prevStartDate,
            @Param("prevEndDate") Instant prevEndDate);

    /**
     * Same shape as getAllLabsSummaryWithDateRange/getLabPerformanceSummary, but sources
     * testCount/patientCount/pendingSamples/reportsGenerated/revenue from the daily_lab_stats
     * rollup (pre-aggregated, kept in sync by DashboardRollupService) instead of live-joining
     * billing/lab_billing/patient_visits/visit_test_result. Only avgTatHours still needs a
     * live join (the rollup doesn't track it), and that join is now against lab_report alone
     * via idx_lab_report_visit_id_lab_id instead of the old 4-table join.
     * Measured: ~600ms per call (x2, once each for dashboardSummary and labPerformance) ->
     * ~280ms for a single shared call, reused by buildKpis/buildDashboardSummary/buildLabPerformance.
     */
    @Query(value =
        "SELECT l.lab_id AS labId, l.name AS labName, " +
        "COALESCE(curr.revenue, 0) AS revenue, " +
        "COALESCE(prev.revenue, 0) AS previousRevenue, " +
        "COALESCE(curr.testCount, 0) AS testCount, " +
        "COALESCE(curr.patientCount, 0) AS patientCount, " +
        "COALESCE(curr.pendingSamples, 0) AS pendingSamples, " +
        "COALESCE(curr.reportsGenerated, 0) AS reportsGenerated, " +
        "ROUND(COALESCE(tat.avgTatHours, 0.0), 1) AS avgTatHours " +
        "FROM labs l " +
        "LEFT JOIN ( " +
        "    SELECT lab_id, " +
        "        SUM(paid_revenue) AS revenue, " +
        "        SUM(test_count) AS testCount, " +
        "        SUM(patient_count) AS patientCount, " +
        "        SUM(pending_samples) AS pendingSamples, " +
        "        SUM(reports_generated) AS reportsGenerated " +
        "    FROM daily_lab_stats " +
        "    WHERE stat_date BETWEEN :startDateLocal AND :endDateLocal " +
        "    GROUP BY lab_id " +
        ") curr ON curr.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lab_id, SUM(paid_revenue) AS revenue " +
        "    FROM daily_lab_stats " +
        "    WHERE stat_date BETWEEN :prevStartDateLocal AND :prevEndDateLocal " +
        "    GROUP BY lab_id " +
        ") prev ON prev.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lv.lab_id, " +
        "        AVG(EXTRACT(EPOCH FROM (r.created_at::timestamptz - v.created_at::timestamptz)) / 3600.0) AS avgTatHours " +
        "    FROM lab_visit lv " +
        "    JOIN patient_visits v ON v.visit_id = lv.visit_id " +
        "    JOIN lab_report r ON r.visit_id = v.visit_id AND r.lab_id = lv.lab_id " +
        "    WHERE v.created_at BETWEEN :startDate AND :endDate AND LOWER(v.visit_status) != 'cancelled' " +
        "    GROUP BY lv.lab_id " +
        ") tat ON tat.lab_id = l.lab_id " +
        "WHERE l.created_by = :createdById " +
        "ORDER BY revenue DESC", nativeQuery = true)
    List<LabPerformanceSummaryProjection> getLabWiseRollupWithDateRange(
            @Param("createdById") Long createdById,
            @Param("startDateLocal") java.time.LocalDate startDateLocal,
            @Param("endDateLocal") java.time.LocalDate endDateLocal,
            @Param("prevStartDateLocal") java.time.LocalDate prevStartDateLocal,
            @Param("prevEndDateLocal") java.time.LocalDate prevEndDateLocal,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query(value =
        "SELECT l.lab_id AS labId, l.name AS labName, " +
        "COALESCE(curr.revenue, 0) AS revenue, " +
        "COALESCE(prev.revenue, 0) AS previousRevenue, " +
        "COALESCE(vstats.testCount, 0) AS testCount, " +
        "COALESCE(vstats.patientCount, 0) AS patientCount, " +
        "COALESCE(vstats.pendingSamples, 0) AS pendingSamples, " +
        "COALESCE(vstats.reportsGenerated, 0) AS reportsGenerated, " +
        "ROUND(COALESCE(vstats.avgTatHours, 0.0), 1) AS avgTatHours " +
        "FROM labs l " +
        "LEFT JOIN ( " +
        "    SELECT lb.lab_id, SUM(b.actual_received_amount::numeric) AS revenue" +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    JOIN patient_visits pv ON pv.billing_id = b.billing_id " +
        "    WHERE b.created_at BETWEEN :startDate AND :endDate AND LOWER(pv.visit_status) != 'cancelled' " +
        "    GROUP BY lb.lab_id " +
        ") curr ON curr.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lb.lab_id, SUM(b.actual_received_amount::numeric) AS revenue" +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    JOIN patient_visits pv ON pv.billing_id = b.billing_id " +
        "    WHERE b.created_at BETWEEN :prevStartDate AND :prevEndDate AND LOWER(pv.visit_status) != 'cancelled' " +
        "    GROUP BY lb.lab_id " +
        ") prev ON prev.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lv.lab_id, " +
        "        COUNT(DISTINCT vtr.id) AS testCount, " +
        "        COUNT(DISTINCT v.patient_id) AS patientCount, " +
        "        COUNT(DISTINCT CASE WHEN v.visit_status = 'Pending' THEN v.visit_id END) AS pendingSamples, " +
        "        COUNT(DISTINCT CASE WHEN vtr.report_status = 'Completed' THEN vtr.id END) AS reportsGenerated, " +
        "        AVG(CASE WHEN r.report_id IS NOT NULL THEN EXTRACT(EPOCH FROM (r.created_at::timestamptz - v.created_at::timestamptz)) / 3600.0 END) AS avgTatHours " +
        "    FROM lab_visit lv " +
        "    JOIN patient_visits v ON v.visit_id = lv.visit_id " +
        "    LEFT JOIN visit_test_result vtr ON vtr.visit_id = v.visit_id AND LOWER(vtr.test_status) = 'active' " +
        "    LEFT JOIN lab_report r ON r.visit_id = v.visit_id AND r.lab_id = lv.lab_id " +
        "    WHERE v.created_at BETWEEN :startDate AND :endDate AND LOWER(v.visit_status) != 'cancelled' " +
        "    GROUP BY lv.lab_id " +
        ") vstats ON vstats.lab_id = l.lab_id " +
        "WHERE l.lab_id = :labId", nativeQuery = true)
    java.util.Optional<LabPerformanceSummaryProjection> getLabPerformanceByLabIdAndDateRange(
            @Param("labId") Long labId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("prevStartDate") Instant prevStartDate,
            @Param("prevEndDate") Instant prevEndDate);

    @Query(value =
        "SELECT l.lab_id AS labId, l.name AS labName, " +
        "COALESCE(curr.revenue, 0) AS revenue, " +
        "0 AS previousRevenue, " +
        "COALESCE(vstats.testCount, 0) AS testCount, " +
        "COALESCE(vstats.patientCount, 0) AS patientCount, " +
        "COALESCE(vstats.pendingSamples, 0) AS pendingSamples, " +
        "COALESCE(vstats.reportsGenerated, 0) AS reportsGenerated, " +
        "ROUND(COALESCE(vstats.avgTatHours, 0.0), 1) AS avgTatHours " +
        "FROM labs l " +
        "LEFT JOIN ( " +
        "    SELECT lb.lab_id, SUM(b.actual_received_amount::numeric) AS revenue" +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    JOIN patient_visits pv ON pv.billing_id = b.billing_id " +
        "    WHERE LOWER(pv.visit_status) != 'cancelled' " +
        "    GROUP BY lb.lab_id " +
        ") curr ON curr.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lv.lab_id, " +
        "        COUNT(DISTINCT vtr.id) AS testCount, " +
        "        COUNT(DISTINCT v.patient_id) AS patientCount, " +
        "        COUNT(DISTINCT CASE WHEN v.visit_status = 'Pending' THEN v.visit_id END) AS pendingSamples, " +
        "        COUNT(DISTINCT CASE WHEN vtr.report_status = 'Completed' THEN vtr.id END) AS reportsGenerated, " +
        "        AVG(CASE WHEN r.report_id IS NOT NULL THEN EXTRACT(EPOCH FROM (r.created_at::timestamptz - v.created_at::timestamptz)) / 3600.0 END) AS avgTatHours " +
        "    FROM lab_visit lv " +
        "    JOIN patient_visits v ON v.visit_id = lv.visit_id " +
        "    LEFT JOIN visit_test_result vtr ON vtr.visit_id = v.visit_id AND LOWER(vtr.test_status) = 'active' " +
        "    LEFT JOIN lab_report r ON r.visit_id = v.visit_id AND r.lab_id = lv.lab_id " +
        "    WHERE LOWER(v.visit_status) != 'cancelled' " +
        "    GROUP BY lv.lab_id " +
        ") vstats ON vstats.lab_id = l.lab_id " +
        "WHERE l.lab_id = :labId", nativeQuery = true)
    java.util.Optional<LabPerformanceSummaryProjection> getLabPerformanceByLabIdAllTime(@Param("labId") Long labId);

    interface LabPerformanceSummaryProjection {
        Long getLabId();
        String getLabName();
        BigDecimal getRevenue();
        BigDecimal getPreviousRevenue();
        Long getTestCount();
        Long getPatientCount();
        Long getPendingSamples();
        Long getReportsGenerated();
        Double getAvgTatHours();
    }
}

