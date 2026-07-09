package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.Doctors;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;


@Repository
public interface DoctorRepository extends JpaRepository<Doctors, Long> {
    boolean existsByEmail(String email);

    Doctors findByEmail(String email);

    OptionalDouble findById(Optional<Long> doctorId);

    // count the number of doctors by labId
    @Query("SELECT COUNT(d) FROM Doctors d JOIN d.labs l WHERE l.id = :labId AND d.createdAt BETWEEN :startDate AND :endDate")
    long countByLabId(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT d.doctor_id AS doctorId, d.name AS doctorName, d.speciality AS speciality, " +
            "COUNT(DISTINCT lv.lab_id) AS labCount, " +
            "COUNT(DISTINCT v.patient_id) AS patientCount, " +
            "COALESCE(SUM(b.total_amount), 0) AS revenue " +
            "FROM doctors d " +
            "JOIN patient_visits v ON v.doctor_id = d.doctor_id " +
            "JOIN lab_visit lv ON lv.visit_id = v.visit_id " +
            "JOIN labs l ON l.lab_id = lv.lab_id " +
            "LEFT JOIN billing b ON v.billing_id = b.billing_id " +
            "WHERE l.created_by = :createdById " +
            "GROUP BY d.doctor_id, d.name, d.speciality " +
            "ORDER BY revenue DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<TopReferringDoctorProjection> getTopReferringDoctors(@Param("createdById") Long createdById, @Param("limit") int limit);

    @Query(value = "SELECT d.doctor_id AS doctorId, d.name AS doctorName, d.speciality AS speciality, " +
            "COUNT(DISTINCT lv.lab_id) AS labCount, " +
            "COUNT(DISTINCT v.patient_id) AS patientCount, " +
            "COALESCE(SUM(b.total_amount), 0) AS revenue " +
            "FROM doctors d " +
            "JOIN patient_visits v ON v.doctor_id = d.doctor_id " +
            "JOIN lab_visit lv ON lv.visit_id = v.visit_id " +
            "JOIN labs l ON l.lab_id = lv.lab_id " +
            "LEFT JOIN billing b ON v.billing_id = b.billing_id " +
            "WHERE l.created_by = :createdById " +
            "AND v.created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY d.doctor_id, d.name, d.speciality " +
            "ORDER BY revenue DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<TopReferringDoctorProjection> getTopReferringDoctorsWithDateRange(@Param("createdById") Long createdById,
                                                                           @Param("startDate") Instant startDate,
                                                                           @Param("endDate") Instant endDate,
                                                                           @Param("limit") int limit);

    interface TopReferringDoctorProjection {
        Long getDoctorId();
        String getDoctorName();
        String getSpeciality();
        Long getLabCount();
        Long getPatientCount();
        BigDecimal getRevenue();
    }
}
