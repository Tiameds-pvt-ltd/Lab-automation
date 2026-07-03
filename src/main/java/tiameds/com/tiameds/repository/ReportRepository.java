package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.com.tiameds.entity.ReportEntity;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    List<ReportEntity> findByVisitIdAndLabId(Long visitId, Long labId);

    @Query(value = "SELECT r.* FROM lab_report r " +
            "JOIN patient_visits v ON r.visit_id = v.visit_id " +
            "WHERE v.patient_id = :patientId AND r.lab_id = :labId " +
            "ORDER BY r.test_name ASC, r.created_at ASC", nativeQuery = true)
    List<ReportEntity> findByPatientIdAndLabId(@Param("patientId") Long patientId, @Param("labId") Long labId);
}
