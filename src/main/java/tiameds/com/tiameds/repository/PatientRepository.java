package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.PatientEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, Long> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<PatientEntity> findAllByLabsId(Long labId);

    Optional<PatientEntity> findByPhoneOrEmail(String phone, String email);

    @Query("SELECT COUNT(p) FROM PatientEntity p JOIN p.labs l WHERE l.id = :labId AND p.createdAt BETWEEN :startDate AND :endDate")
    long countByLabIdAndCreatedAtBetween(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);


    Optional<PatientEntity> findByPhoneOrFirstName(String phone, String firstName);

    @Query("SELECT p FROM PatientEntity p WHERE p.phone = :phone AND p.firstName = :firstName")
    Optional<PatientEntity> findByPhoneAndFirstName(
            @Param("phone") String phone,
            @Param("firstName") String firstName
    );

    Optional<PatientEntity> findFirstByPhoneOrderByPatientIdAsc(String phone);
}
