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


}
