package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.com.tiameds.entity.RadiologyReport;

import java.util.List;
import java.util.Optional;

public interface RadiologyReportRepository extends JpaRepository<RadiologyReport, Long> {

    Optional<RadiologyReport> findByLabIdAndVisitIdAndTestId(Long labId, Long visitId, Long testId);

    List<RadiologyReport> findByLabIdAndVisitIdOrderByIdAsc(Long labId, Long visitId);
}
