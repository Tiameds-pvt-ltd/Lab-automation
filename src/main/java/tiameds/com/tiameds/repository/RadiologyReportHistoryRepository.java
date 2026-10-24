package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.com.tiameds.entity.RadiologyReportHistory;

import java.util.List;

public interface RadiologyReportHistoryRepository extends JpaRepository<RadiologyReportHistory, Long> {

    List<RadiologyReportHistory> findByReportIdOrderByChangedAtDesc(Long reportId);
}
