package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VisitSample;

import java.time.LocalDateTime;

@Repository
public interface VisitSampleRepository extends JpaRepository<VisitSample, Long> {

    @Query("SELECT COUNT(vs) FROM VisitSample vs JOIN vs.visit v JOIN v.labs l WHERE l.createdBy = :createdBy AND v.visitStatus = 'Pending'")
    long countPendingSamplesByLabsCreatedBy(@Param("createdBy") User createdBy);

    @Query("SELECT COUNT(vs) FROM VisitSample vs JOIN vs.visit v JOIN v.labs l WHERE l.createdBy = :createdBy AND v.visitStatus = 'Pending' AND vs.createdAt BETWEEN :startDate AND :endDate")
    long countPendingSamplesByLabsCreatedByAndCreatedAtBetween(@Param("createdBy") User createdBy, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Total collected samples for a particular lab
    @Query("SELECT COUNT(vs) FROM VisitSample vs JOIN vs.visit v JOIN v.labs l WHERE l.id = :labId")
    long countCollectedSamplesByLabId(@Param("labId") Long labId);

    @Query("SELECT COUNT(vs) FROM VisitSample vs JOIN vs.visit v JOIN v.labs l WHERE l.id = :labId AND vs.createdAt BETWEEN :startDate AND :endDate")
    long countCollectedSamplesByLabIdAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

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


























