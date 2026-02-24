package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.ReportRoleSetting;

@Repository
public interface ReportRoleSettingRepository extends JpaRepository<ReportRoleSetting, Long> {
}
