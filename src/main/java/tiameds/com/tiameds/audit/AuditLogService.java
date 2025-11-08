package tiameds.com.tiameds.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tiameds.com.tiameds.entity.LabAuditLogs;
import tiameds.com.tiameds.repository.AuditLogsRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class AuditLogService {

    private final AuditLogsRepository auditLogsRepository;

    public AuditLogService(AuditLogsRepository auditLogsRepository) {
        this.auditLogsRepository = auditLogsRepository;
    }

    @Async
    public void persistAsync(LabAuditLogs auditLog) {
        try {
            if (auditLog.getTimestamp() == null) {
                auditLog.setTimestamp(LocalDateTime.now());
            }
            if (auditLog.getRequestId() == null || auditLog.getRequestId().isEmpty()) {
                auditLog.setRequestId(UUID.randomUUID().toString());
            }
            // normalize nullable text fields to empty strings
            if (auditLog.getEntityId() == null) auditLog.setEntityId("");
            if (auditLog.getEntityType() == null) auditLog.setEntityType("");
            // normalize JSON fields to empty JSON objects
            if (auditLog.getFieldChanged() == null || auditLog.getFieldChanged().isEmpty()) auditLog.setFieldChanged("{}");
            if (auditLog.getOldValue() == null || auditLog.getOldValue().isEmpty()) auditLog.setOldValue("{}");
            if (auditLog.getNewValue() == null || auditLog.getNewValue().isEmpty()) auditLog.setNewValue("{}");
            if (auditLog.getChangeReason() == null) auditLog.setChangeReason("");
            if (auditLog.getIpAddress() == null) auditLog.setIpAddress("");
            if (auditLog.getDeviceInfo() == null) auditLog.setDeviceInfo("");
            if (auditLog.getRole() == null) auditLog.setRole("");
            if (auditLog.getModule() == null) auditLog.setModule("");
            if (auditLog.getActionType() == null) auditLog.setActionType("");
            if (auditLog.getUsername() == null) auditLog.setUsername("");

            auditLogsRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Failed to persist audit log: {}", ex.getMessage(), ex);
        }
    }
}


