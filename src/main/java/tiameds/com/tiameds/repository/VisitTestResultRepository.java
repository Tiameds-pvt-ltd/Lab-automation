package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tiameds.com.tiameds.entity.VisitTestResult;

import java.util.Optional;

public interface VisitTestResultRepository extends JpaRepository<VisitTestResult , Long> {

    @Query("SELECT vtr FROM VisitTestResult vtr WHERE vtr.visit.visitId = :visitId AND vtr.test.id = :testId")
    Optional<VisitTestResult> findByVisitIdAndTestId(Long visitId, Long testId);
}
