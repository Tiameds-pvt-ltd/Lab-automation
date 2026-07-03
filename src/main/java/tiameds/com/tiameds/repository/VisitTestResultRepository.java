package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VisitTestResult;

import java.time.LocalDateTime;
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

    // Count completed reports across all labs created by a superadmin
    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE l.createdBy = :createdBy AND vtr.reportStatus = 'Completed'")
    long countCompletedReportsByLabsCreatedBy(@Param("createdBy") User createdBy);

    @Query("SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit.labs l WHERE l.createdBy = :createdBy AND vtr.reportStatus = 'Completed' AND vtr.createdAt BETWEEN :startDate AND :endDate")
    long countCompletedReportsByLabsCreatedByAndCreatedAtBetween(@Param("createdBy") User createdBy, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
