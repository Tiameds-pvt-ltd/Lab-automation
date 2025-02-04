package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.Doctors;

import java.util.Optional;
import java.util.OptionalDouble;


@Repository
public interface DoctorRepository extends JpaRepository<Doctors, Long> {
    boolean existsByEmail(String email);

    Doctors findByEmail(String email);

    OptionalDouble findById(Optional<Long> doctorId);
}
