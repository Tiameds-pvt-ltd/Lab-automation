package tiameds.com.tiameds.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "lab_audit_logs")
public class LabAuditLogs {

    @Id
    @GeneratedValue
    private UUID id; // Unique identifier (UUID)

    @Column(nullable = false)
    private LocalDateTime timestamp; // Event time (UTC)

    @Column
    private Long userId; // ID of the actor (nullable; DB column is BIGINT)

    @Column(name = "lab_id", nullable = false)
    private String lab_id;

    private String username; // Display name (optional)

    private String role; // Role of user

    private String ipAddress; // Source IP address

    private String module; // Affected module (Pharmacy, Lab, etc.)

    private String entityType; // Object category (Patient, Invoice)

    private String entityId; // Specific object reference

    private String actionType; // Action performed (VIEW, CREATE, UPDATE, DELETE)

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String fieldChanged; // Specific field modified (if any) - stored as JSON

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String oldValue; // Previous value (if applicable) - stored as JSON

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String newValue; // New value (if applicable) - stored as JSON

    @Column(columnDefinition = "TEXT")
    private String changeReason; // Reason for change (user-entered)

    private String requestId; // Correlated request ID for session

    @Column(columnDefinition = "TEXT")
    private String deviceInfo; // Client device info

    @Enumerated(EnumType.STRING)
    private Severity severity; // LOW / MEDIUM / HIGH / CRITICAL

    // Enum for severity
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
