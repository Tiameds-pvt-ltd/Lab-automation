package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.HealthPackage;
import tiameds.com.tiameds.entity.Lab;

import java.time.LocalDateTime;
import java.util.List;

@Transactional
@Repository
public interface HealthPackageRepository extends JpaRepository<HealthPackage, Long> {

    List<HealthPackage> findByLabs_Id(Long labId);

    boolean existsByPackageName(String packageName);

    List<HealthPackage> findAllByLabs(Lab lab);

    @Query("SELECT COUNT(h) FROM HealthPackage h JOIN h.labs l WHERE l.id = :labId AND h.createdAt BETWEEN :startDate AND :endDate")
    long countByLabId(@Param("labId") Long labId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
