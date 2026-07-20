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

    List<Lab> findByCreatedBy(User currentUser);

    @Query("SELECT l FROM Lab l JOIN FETCH l.members WHERE l.id = :id")
    Optional<Lab> findLabWithMembers(@Param("id") long id);

    Optional<Lab> findById(Long id);


    Optional<Lab> findByMembers(User user);

    @Transactional
    @Query("SELECT l FROM Lab l JOIN l.members m WHERE m.id = :userId")
    Set<Lab> findLabsByUserId(@Param("userId") Long userId);

    long countByCreatedBy(User createdBy);

    @Query("SELECT COUNT(l) FROM Lab l WHERE l.createdBy = :createdBy AND l.createdAt BETWEEN :startDate AND :endDate")
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
        "    SELECT lb.lab_id, SUM(b.total_amount) AS revenue " +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    WHERE b.created_at BETWEEN :startDate AND :endDate " +
        "    GROUP BY lb.lab_id " +
        ") curr ON curr.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lb.lab_id, SUM(b.total_amount) AS revenue " +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    WHERE b.created_at BETWEEN :prevStartDate AND :prevEndDate " +
        "    GROUP BY lb.lab_id " +
        ") prev ON prev.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lv.lab_id, " +
        "        COUNT(DISTINCT vtr.id) AS testCount, " +
        "        COUNT(DISTINCT v.patient_id) AS patientCount, " +
        "        COUNT(DISTINCT CASE WHEN v.visit_status = 'Pending' THEN v.visit_id END) AS pendingSamples, " +
        "        COUNT(DISTINCT CASE WHEN vtr.report_status = 'Completed' THEN vtr.id END) AS reportsGenerated, " +
        "        AVG(CASE WHEN r.report_id IS NOT NULL THEN EXTRACT(EPOCH FROM (r.created_at - v.created_at)) / 3600.0 END) AS avgTatHours " +
        "    FROM lab_visit lv " +
        "    JOIN patient_visits v ON v.visit_id = lv.visit_id " +
        "    LEFT JOIN visit_test_result vtr ON vtr.visit_id = v.visit_id " +
        "    LEFT JOIN lab_report r ON r.visit_id = v.visit_id AND r.lab_id = lv.lab_id " +
        "    WHERE v.created_at BETWEEN :startDate AND :endDate " +
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
        "    SELECT lb.lab_id, SUM(b.total_amount) AS revenue " +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    GROUP BY lb.lab_id " +
        ") curr ON curr.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lv.lab_id, " +
        "        COUNT(DISTINCT vtr.id) AS testCount, " +
        "        COUNT(DISTINCT v.patient_id) AS patientCount, " +
        "        COUNT(DISTINCT CASE WHEN v.visit_status = 'Pending' THEN v.visit_id END) AS pendingSamples, " +
        "        COUNT(DISTINCT CASE WHEN vtr.report_status = 'Completed' THEN vtr.id END) AS reportsGenerated, " +
        "        AVG(CASE WHEN r.report_id IS NOT NULL THEN EXTRACT(EPOCH FROM (r.created_at - v.created_at)) / 3600.0 END) AS avgTatHours " +
        "    FROM lab_visit lv " +
        "    JOIN patient_visits v ON v.visit_id = lv.visit_id " +
        "    LEFT JOIN visit_test_result vtr ON vtr.visit_id = v.visit_id " +
        "    LEFT JOIN lab_report r ON r.visit_id = v.visit_id AND r.lab_id = lv.lab_id " +
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
        "    SELECT lb.lab_id, SUM(b.total_amount) AS revenue " +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    GROUP BY lb.lab_id " +
        ") curr ON curr.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lv.lab_id, " +
        "        COUNT(DISTINCT vtr.id) AS testCount, " +
        "        COUNT(DISTINCT v.patient_id) AS patientCount, " +
        "        COUNT(DISTINCT CASE WHEN v.visit_status = 'Pending' THEN v.visit_id END) AS pendingSamples, " +
        "        COUNT(DISTINCT CASE WHEN vtr.report_status = 'Completed' THEN vtr.id END) AS reportsGenerated, " +
        "        AVG(CASE WHEN r.report_id IS NOT NULL THEN EXTRACT(EPOCH FROM (r.created_at - v.created_at)) / 3600.0 END) AS avgTatHours " +
        "    FROM lab_visit lv " +
        "    JOIN patient_visits v ON v.visit_id = lv.visit_id " +
        "    LEFT JOIN visit_test_result vtr ON vtr.visit_id = v.visit_id " +
        "    LEFT JOIN lab_report r ON r.visit_id = v.visit_id AND r.lab_id = lv.lab_id " +
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
        "    SELECT lb.lab_id, SUM(b.total_amount) AS revenue " +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    WHERE b.created_at BETWEEN :startDate AND :endDate " +
        "    GROUP BY lb.lab_id " +
        ") curr ON curr.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lb.lab_id, SUM(b.total_amount) AS revenue " +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    WHERE b.created_at BETWEEN :prevStartDate AND :prevEndDate " +
        "    GROUP BY lb.lab_id " +
        ") prev ON prev.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lv.lab_id, " +
        "        COUNT(DISTINCT vtr.id) AS testCount, " +
        "        COUNT(DISTINCT v.patient_id) AS patientCount, " +
        "        COUNT(DISTINCT CASE WHEN v.visit_status = 'Pending' THEN v.visit_id END) AS pendingSamples, " +
        "        COUNT(DISTINCT CASE WHEN vtr.report_status = 'Completed' THEN vtr.id END) AS reportsGenerated, " +
        "        AVG(CASE WHEN r.report_id IS NOT NULL THEN EXTRACT(EPOCH FROM (r.created_at - v.created_at)) / 3600.0 END) AS avgTatHours " +
        "    FROM lab_visit lv " +
        "    JOIN patient_visits v ON v.visit_id = lv.visit_id " +
        "    LEFT JOIN visit_test_result vtr ON vtr.visit_id = v.visit_id " +
        "    LEFT JOIN lab_report r ON r.visit_id = v.visit_id AND r.lab_id = lv.lab_id " +
        "    WHERE v.created_at BETWEEN :startDate AND :endDate " +
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
        "    SELECT lb.lab_id, SUM(b.total_amount) AS revenue " +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    WHERE b.created_at BETWEEN :startDate AND :endDate " +
        "    GROUP BY lb.lab_id " +
        ") curr ON curr.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lb.lab_id, SUM(b.total_amount) AS revenue " +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    WHERE b.created_at BETWEEN :prevStartDate AND :prevEndDate " +
        "    GROUP BY lb.lab_id " +
        ") prev ON prev.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lv.lab_id, " +
        "        COUNT(DISTINCT vtr.id) AS testCount, " +
        "        COUNT(DISTINCT v.patient_id) AS patientCount, " +
        "        COUNT(DISTINCT CASE WHEN v.visit_status = 'Pending' THEN v.visit_id END) AS pendingSamples, " +
        "        COUNT(DISTINCT CASE WHEN vtr.report_status = 'Completed' THEN vtr.id END) AS reportsGenerated, " +
        "        AVG(CASE WHEN r.report_id IS NOT NULL THEN EXTRACT(EPOCH FROM (r.created_at - v.created_at)) / 3600.0 END) AS avgTatHours " +
        "    FROM lab_visit lv " +
        "    JOIN patient_visits v ON v.visit_id = lv.visit_id " +
        "    LEFT JOIN visit_test_result vtr ON vtr.visit_id = v.visit_id " +
        "    LEFT JOIN lab_report r ON r.visit_id = v.visit_id AND r.lab_id = lv.lab_id " +
        "    WHERE v.created_at BETWEEN :startDate AND :endDate " +
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
        "    SELECT lb.lab_id, SUM(b.total_amount) AS revenue " +
        "    FROM billing b JOIN lab_billing lb ON lb.billing_id = b.billing_id " +
        "    GROUP BY lb.lab_id " +
        ") curr ON curr.lab_id = l.lab_id " +
        "LEFT JOIN ( " +
        "    SELECT lv.lab_id, " +
        "        COUNT(DISTINCT vtr.id) AS testCount, " +
        "        COUNT(DISTINCT v.patient_id) AS patientCount, " +
        "        COUNT(DISTINCT CASE WHEN v.visit_status = 'Pending' THEN v.visit_id END) AS pendingSamples, " +
        "        COUNT(DISTINCT CASE WHEN vtr.report_status = 'Completed' THEN vtr.id END) AS reportsGenerated, " +
        "        AVG(CASE WHEN r.report_id IS NOT NULL THEN EXTRACT(EPOCH FROM (r.created_at - v.created_at)) / 3600.0 END) AS avgTatHours " +
        "    FROM lab_visit lv " +
        "    JOIN patient_visits v ON v.visit_id = lv.visit_id " +
        "    LEFT JOIN visit_test_result vtr ON vtr.visit_id = v.visit_id " +
        "    LEFT JOIN lab_report r ON r.visit_id = v.visit_id AND r.lab_id = lv.lab_id " +
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

