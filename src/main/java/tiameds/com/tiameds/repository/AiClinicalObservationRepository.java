package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.com.tiameds.entity.AiClinicalObservation;

import java.util.List;
import java.util.Optional;

public interface AiClinicalObservationRepository extends JpaRepository<AiClinicalObservation, Long> {

    List<AiClinicalObservation> findByVisit_VisitId(Long visitId);

    Optional<AiClinicalObservation> findTopByVisit_VisitIdOrderByCreatedAtDesc(Long visitId);
}
