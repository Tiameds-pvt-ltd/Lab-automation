package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.Doctors;

import java.time.LocalDateTime;
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
}
