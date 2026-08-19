package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.VisitTest;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisitTestRepository extends JpaRepository<VisitTest, Long> {

    @Query("SELECT vt FROM VisitTest vt WHERE vt.test.id = :testId AND vt.testName IS NOT NULL ORDER BY vt.id DESC")
    List<VisitTest> findByTestIdOrderByIdDesc(@Param("testId") Long testId);

    @Query("SELECT vt FROM VisitTest vt WHERE vt.visit.id = :visitId AND vt.test.id = :testId")
    Optional<VisitTest> findByVisitIdAndTestId(@Param("visitId") Long visitId, @Param("testId") Long testId);
}
