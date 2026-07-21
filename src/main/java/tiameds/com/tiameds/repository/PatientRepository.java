package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.PatientEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, Long> {
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    List<PatientEntity> findAllByLabsId(Long labId);
    Optional<PatientEntity> findByPhoneOrEmail(String phone, String email);

    @Query("SELECT COUNT(p) FROM PatientEntity p JOIN p.labs l WHERE l.id = :labId")
    long countByLabId(@Param("labId") Long labId);

    @Query("SELECT COUNT(p) FROM PatientEntity p JOIN p.labs l WHERE l.id = :labId AND p.createdAt BETWEEN :startDate AND :endDate")
    long countByLabIdAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    Optional<PatientEntity> findByPhoneOrFirstName(String phone, String firstName);

    @Query("SELECT p FROM PatientEntity p WHERE p.phone = :phone AND p.firstName = :firstName")
    Optional<PatientEntity> findByPhoneAndFirstName(@Param("phone") String phone, @Param("firstName") String firstName);

    Optional<PatientEntity> findFirstByPhoneOrderByPatientIdAsc(String phone);

    @Query("SELECT p FROM PatientEntity p JOIN p.labs l WHERE p.phone = :phone AND p.firstName = :firstName AND l.id = :id")
    Optional<PatientEntity> findByPhoneAndFirstNameAndLabsId(String phone, String firstName, long id);


    @Query("SELECT p FROM PatientEntity p JOIN p.labs l WHERE p.phone = :phone AND l.id = :labId")
    List<PatientEntity> findByPhoneAndLabId(@Param("phone") String phone, @Param("labId") Long labId);


    @Query("SELECT p FROM PatientEntity p JOIN p.labs l WHERE p.phone LIKE CONCAT(:phonePrefix, '%') AND l.id = :labId")
    List<PatientEntity> findByPhoneStartingWithAndLabId(@Param("phonePrefix") String phonePrefix, @Param("labId") Long labId);

    boolean existsByPatientIdAndLabsId(Long patientId, Long labId);

    // Gender distribution for a lab
    @Query(value =
        "SELECT LOWER(p.gender) AS gender, COUNT(*) AS count " +
        "FROM patients p " +
        "JOIN lab_patients pl ON p.patient_id = pl.patient_id " +
        "WHERE pl.lab_id = :labId AND p.gender IS NOT NULL " +
        "GROUP BY LOWER(p.gender)", nativeQuery = true)
    List<GenderCountProjection> countByGenderForLab(@Param("labId") Long labId);

    @Query(value =
        "SELECT LOWER(p.gender) AS gender, COUNT(*) AS count " +
        "FROM patients p " +
        "JOIN lab_patients pl ON p.patient_id = pl.patient_id " +
        "WHERE pl.lab_id = :labId AND p.gender IS NOT NULL " +
        "AND p.created_at BETWEEN :startDate AND :endDate " +
        "GROUP BY LOWER(p.gender)", nativeQuery = true)
    List<GenderCountProjection> countByGenderForLabAndDateRange(
            @Param("labId") Long labId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    // Age group distribution for a lab (age derived from date_of_birth)
    @Query(value =
        "SELECT sub.ageGroup AS ageGroup, COUNT(*) AS count " +
        "FROM ( " +
        "  SELECT CASE " +
        "    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_of_birth)) BETWEEN 0  AND 18 THEN '0-18' " +
        "    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_of_birth)) BETWEEN 19 AND 35 THEN '19-35' " +
        "    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_of_birth)) BETWEEN 36 AND 50 THEN '36-50' " +
        "    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_of_birth)) BETWEEN 51 AND 65 THEN '51-65' " +
        "    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_of_birth)) > 65              THEN '65+' " +
        "    ELSE NULL END AS ageGroup, " +
        "  EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_of_birth))::INTEGER AS ageNum " +
        "  FROM patients p " +
        "  JOIN lab_patients pl ON p.patient_id = pl.patient_id " +
        "  WHERE pl.lab_id = :labId AND p.date_of_birth IS NOT NULL " +
        ") sub " +
        "WHERE sub.ageGroup IS NOT NULL " +
        "GROUP BY sub.ageGroup " +
        "ORDER BY MIN(sub.ageNum)", nativeQuery = true)
    List<AgeGroupCountProjection> countByAgeGroupForLab(@Param("labId") Long labId);

    @Query(value =
        "SELECT sub.ageGroup AS ageGroup, COUNT(*) AS count " +
        "FROM ( " +
        "  SELECT CASE " +
        "    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_of_birth)) BETWEEN 0  AND 18 THEN '0-18' " +
        "    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_of_birth)) BETWEEN 19 AND 35 THEN '19-35' " +
        "    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_of_birth)) BETWEEN 36 AND 50 THEN '36-50' " +
        "    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_of_birth)) BETWEEN 51 AND 65 THEN '51-65' " +
        "    WHEN EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_of_birth)) > 65              THEN '65+' " +
        "    ELSE NULL END AS ageGroup, " +
        "  EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_of_birth))::INTEGER AS ageNum " +
        "  FROM patients p " +
        "  JOIN lab_patients pl ON p.patient_id = pl.patient_id " +
        "  WHERE pl.lab_id = :labId AND p.date_of_birth IS NOT NULL " +
        "  AND p.created_at BETWEEN :startDate AND :endDate " +
        ") sub " +
        "WHERE sub.ageGroup IS NOT NULL " +
        "GROUP BY sub.ageGroup " +
        "ORDER BY MIN(sub.ageNum)", nativeQuery = true)
    List<AgeGroupCountProjection> countByAgeGroupForLabAndDateRange(
            @Param("labId") Long labId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    interface GenderCountProjection {
        String getGender();
        Long getCount();
    }

    interface AgeGroupCountProjection {
        String getAgeGroup();
        Long getCount();
    }
}
