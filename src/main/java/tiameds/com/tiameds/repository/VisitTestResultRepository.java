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
}
