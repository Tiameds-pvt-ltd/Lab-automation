package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tiameds.com.tiameds.entity.RadiologyTemplate;

import java.util.List;
import java.util.Optional;

public interface RadiologyTemplateRepository extends JpaRepository<RadiologyTemplate, Long> {

    Optional<RadiologyTemplate> findByTestIdAndLabIdAndIsActiveTrue(Long testId, Long labId);

    Optional<RadiologyTemplate> findByTestIdAndLabIdIsNullAndIsActiveTrue(Long testId);

    @Query("SELECT COALESCE(MAX(t.version), 0) FROM RadiologyTemplate t WHERE t.testId = :testId AND t.labId = :labId")
    int findMaxVersionForLab(@Param("testId") Long testId, @Param("labId") Long labId);

    @Query("SELECT COALESCE(MAX(t.version), 0) FROM RadiologyTemplate t WHERE t.testId = :testId AND t.labId IS NULL")
    int findMaxVersionGlobal(@Param("testId") Long testId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE RadiologyTemplate t SET t.isActive = false WHERE t.testId = :testId AND t.labId = :labId AND t.isActive = true")
    int deactivateActiveForLab(@Param("testId") Long testId, @Param("labId") Long labId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE RadiologyTemplate t SET t.isActive = false WHERE t.testId = :testId AND t.labId IS NULL AND t.isActive = true")
    int deactivateActiveGlobal(@Param("testId") Long testId);

    // Active templates a lab can use: its own overrides plus the global ones.
    @Query("SELECT t FROM RadiologyTemplate t WHERE t.isActive = true AND (t.labId = :labId OR t.labId IS NULL) ORDER BY t.testId, t.name")
    List<RadiologyTemplate> findActiveVisibleToLab(@Param("labId") Long labId);

    @Query("SELECT t FROM RadiologyTemplate t WHERE t.isActive = true AND t.testId = :testId AND (t.labId = :labId OR t.labId IS NULL) ORDER BY t.name")
    List<RadiologyTemplate> findActiveVisibleToLabForTest(@Param("labId") Long labId, @Param("testId") Long testId);
}
