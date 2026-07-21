package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VisitSample;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitSampleRepository extends JpaRepository<VisitSample, Long> {

    @Query("SELECT COUNT(vs) FROM VisitSample vs JOIN vs.visit v JOIN v.labs l WHERE l.createdBy = :createdBy AND v.visitStatus = 'Pending'")
    long countPendingSamplesByLabsCreatedBy(@Param("createdBy") User createdBy);

    @Query("SELECT COUNT(vs) FROM VisitSample vs JOIN vs.visit v JOIN v.labs l WHERE l.createdBy = :createdBy AND v.visitStatus = 'Pending' AND vs.createdAt BETWEEN :startDate AND :endDate")
    long countPendingSamplesByLabsCreatedByAndCreatedAtBetween(@Param("createdBy") User createdBy, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Total collected samples for a particular lab (via patient.labs path — consistent with funnel queries)
    @Query("SELECT COUNT(vs) FROM VisitSample vs JOIN vs.visit v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId")
    long countCollectedSamplesByLabIdViaPatient(@Param("labId") Long labId);

    @Query("SELECT COUNT(vs) FROM VisitSample vs JOIN vs.visit v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId AND vs.createdAt BETWEEN :startDate AND :endDate")
    long countCollectedSamplesByLabIdViaPatientAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Total collected samples for a particular lab
    @Query("SELECT COUNT(vs) FROM VisitSample vs JOIN vs.visit v JOIN v.labs l WHERE l.id = :labId")
    long countCollectedSamplesByLabId(@Param("labId") Long labId);

    // Count distinct visits that have at least one sample collected
    @Query("SELECT COUNT(DISTINCT v.visitId) FROM VisitSample vs JOIN vs.visit v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId")
    long countDistinctVisitsWithSamplesByLabId(@Param("labId") Long labId);

    @Query("SELECT COUNT(DISTINCT v.visitId) FROM VisitSample vs JOIN vs.visit v JOIN v.patient p JOIN p.labs l WHERE l.id = :labId AND v.createdAt BETWEEN :startDate AND :endDate")
    long countDistinctVisitsWithSamplesByLabIdAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    @Query("SELECT COUNT(vs) FROM VisitSample vs JOIN vs.visit v JOIN v.labs l WHERE l.id = :labId AND vs.createdAt BETWEEN :startDate AND :endDate")
    long countCollectedSamplesByLabIdAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Technician performance: samples processed, reports entered, avg TAT — for a specific lab
    @Query(value =
        "SELECT (u.first_name || ' ' || u.last_name) AS technicianName, " +
        "  vs_agg.samplesProcessed AS samplesProcessed, " +
        "  COALESCE(vtr_agg.reportsEntered, 0) AS reportsEntered, " +
        "  ROUND(COALESCE(tat_agg.avgTatHours, 0)::numeric, 1) AS avgTatHours " +
        "FROM ( " +
        "  SELECT vs.created_by AS username, COUNT(*) AS samplesProcessed " +
        "  FROM patient_visit_sample vs " +
        "  JOIN patient_visits pv ON vs.visit_id = pv.visit_id " +
        "  JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
        "  WHERE lv.lab_id = :labId AND vs.created_by IS NOT NULL " +
        "  GROUP BY vs.created_by " +
        ") vs_agg " +
        "JOIN users u ON u.username = vs_agg.username " +
        "JOIN users_roles ur ON ur.user_id = u.user_id " +
        "JOIN roles r ON r.role_id = ur.role_id AND r.name = 'TECHNICIAN' " +
        "LEFT JOIN ( " +
        "  SELECT vtr.updated_by AS username, COUNT(*) AS reportsEntered " +
        "  FROM visit_test_result vtr " +
        "  JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
        "  JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
        "  WHERE lv.lab_id = :labId AND vtr.is_filled = true AND vtr.updated_by IS NOT NULL " +
        "  GROUP BY vtr.updated_by " +
        ") vtr_agg ON vtr_agg.username = vs_agg.username " +
        "LEFT JOIN ( " +
        "  SELECT vs2.created_by AS username, " +
        "    AVG(EXTRACT(EPOCH FROM (vtr2.updated_at - vs2.created_at)) / 3600.0) AS avgTatHours " +
        "  FROM patient_visit_sample vs2 " +
        "  JOIN patient_visits pv2 ON vs2.visit_id = pv2.visit_id " +
        "  JOIN lab_visit lv2 ON pv2.visit_id = lv2.visit_id " +
        "  JOIN visit_test_result vtr2 ON vtr2.visit_id = pv2.visit_id AND vtr2.is_filled = true " +
        "  WHERE lv2.lab_id = :labId AND vs2.created_by IS NOT NULL " +
        "  GROUP BY vs2.created_by " +
        ") tat_agg ON tat_agg.username = vs_agg.username " +
        "ORDER BY samplesProcessed DESC " +
        "LIMIT :limit", nativeQuery = true)
    List<TechnicianPerformanceProjection> getTechnicianPerformanceByLabId(
            @Param("labId") Long labId,
            @Param("limit") int limit);

    @Query(value =
        "SELECT (u.first_name || ' ' || u.last_name) AS technicianName, " +
        "  vs_agg.samplesProcessed AS samplesProcessed, " +
        "  COALESCE(vtr_agg.reportsEntered, 0) AS reportsEntered, " +
        "  ROUND(COALESCE(tat_agg.avgTatHours, 0)::numeric, 1) AS avgTatHours " +
        "FROM ( " +
        "  SELECT vs.created_by AS username, COUNT(*) AS samplesProcessed " +
        "  FROM patient_visit_sample vs " +
        "  JOIN patient_visits pv ON vs.visit_id = pv.visit_id " +
        "  JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
        "  WHERE lv.lab_id = :labId AND vs.created_by IS NOT NULL " +
        "    AND vs.created_at BETWEEN :startDate AND :endDate " +
        "  GROUP BY vs.created_by " +
        ") vs_agg " +
        "JOIN users u ON u.username = vs_agg.username " +
        "JOIN users_roles ur ON ur.user_id = u.user_id " +
        "JOIN roles r ON r.role_id = ur.role_id AND r.name = 'TECHNICIAN' " +
        "LEFT JOIN ( " +
        "  SELECT vtr.updated_by AS username, COUNT(*) AS reportsEntered " +
        "  FROM visit_test_result vtr " +
        "  JOIN patient_visits pv ON vtr.visit_id = pv.visit_id " +
        "  JOIN lab_visit lv ON pv.visit_id = lv.visit_id " +
        "  WHERE lv.lab_id = :labId AND vtr.is_filled = true AND vtr.updated_by IS NOT NULL " +
        "    AND vtr.updated_at BETWEEN :startDate AND :endDate " +
        "  GROUP BY vtr.updated_by " +
        ") vtr_agg ON vtr_agg.username = vs_agg.username " +
        "LEFT JOIN ( " +
        "  SELECT vs2.created_by AS username, " +
        "    AVG(EXTRACT(EPOCH FROM (vtr2.updated_at - vs2.created_at)) / 3600.0) AS avgTatHours " +
        "  FROM patient_visit_sample vs2 " +
        "  JOIN patient_visits pv2 ON vs2.visit_id = pv2.visit_id " +
        "  JOIN lab_visit lv2 ON pv2.visit_id = lv2.visit_id " +
        "  JOIN visit_test_result vtr2 ON vtr2.visit_id = pv2.visit_id AND vtr2.is_filled = true " +
        "  WHERE lv2.lab_id = :labId AND vs2.created_by IS NOT NULL " +
        "    AND vs2.created_at BETWEEN :startDate AND :endDate " +
        "  GROUP BY vs2.created_by " +
        ") tat_agg ON tat_agg.username = vs_agg.username " +
        "ORDER BY samplesProcessed DESC " +
        "LIMIT :limit", nativeQuery = true)
    List<TechnicianPerformanceProjection> getTechnicianPerformanceByLabIdAndDateRange(
            @Param("labId") Long labId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("limit") int limit);

    interface TechnicianPerformanceProjection {
        String getTechnicianName();
        Long getSamplesProcessed();
        Long getReportsEntered();
        Double getAvgTatHours();
    }

    // Collected samples by a specific role for a particular lab
    @Query(value = "SELECT COUNT(vs.id) FROM patient_visit_sample vs " +
            "JOIN patient_visits v ON vs.visit_id = v.visit_id " +
            "JOIN lab_visit lv ON v.visit_id = lv.visit_id " +
            "JOIN labs l ON lv.lab_id = l.lab_id " +
            "JOIN users u ON vs.created_by = u.username " +
            "JOIN users_roles ur ON u.user_id = ur.user_id " +
            "JOIN roles r ON ur.role_id = r.role_id " +
            "WHERE l.lab_id = :labId AND r.name = :roleName", nativeQuery = true)
    long countCollectedSamplesByLabIdAndRole(@Param("labId") Long labId, @Param("roleName") String roleName);

    @Query(value = "SELECT COUNT(vs.id) FROM patient_visit_sample vs " +
            "JOIN patient_visits v ON vs.visit_id = v.visit_id " +
            "JOIN lab_visit lv ON v.visit_id = lv.visit_id " +
            "JOIN labs l ON lv.lab_id = l.lab_id " +
            "JOIN users u ON vs.created_by = u.username " +
            "JOIN users_roles ur ON u.user_id = ur.user_id " +
            "JOIN roles r ON ur.role_id = r.role_id " +
            "WHERE l.lab_id = :labId AND r.name = :roleName " +
            "AND vs.created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    long countCollectedSamplesByLabIdAndRoleAndCreatedAtBetween(@Param("labId") Long labId, @Param("roleName") String roleName, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}


























