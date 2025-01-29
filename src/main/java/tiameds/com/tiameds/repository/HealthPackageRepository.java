package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.HealthPackage;
import tiameds.com.tiameds.entity.Lab;

import java.util.List;

@Transactional
@Repository
public interface HealthPackageRepository extends JpaRepository<HealthPackage, Long> {

    List<HealthPackage> findByLabs_Id(Long labId);

    boolean existsByPackageName(String packageName);

    List<HealthPackage> findAllByLabs(Lab lab);
}
