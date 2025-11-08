package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.com.tiameds.entity.LabAuditLogs;

public interface AuditLogsRepository extends JpaRepository<LabAuditLogs, java.util.UUID> {

}
