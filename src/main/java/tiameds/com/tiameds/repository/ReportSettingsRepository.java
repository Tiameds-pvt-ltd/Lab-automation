package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.ReportSettings;

import java.util.Optional;

@Repository
public interface ReportSettingsRepository extends JpaRepository<ReportSettings, Long> {

    Optional<ReportSettings> findByLab_Id(Long labId);

    boolean existsByLab_Id(Long labId);

    @Query("SELECT rs FROM ReportSettings rs LEFT JOIN FETCH rs.roles WHERE rs.lab.id = :labId")
    Optional<ReportSettings> findByLabIdWithRoles(@Param("labId") Long labId);
}
