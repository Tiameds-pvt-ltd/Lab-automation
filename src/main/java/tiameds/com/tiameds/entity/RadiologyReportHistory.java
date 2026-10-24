package tiameds.com.tiameds.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Append-only copy of a radiology report as it was BEFORE an amendment.
 * Rows are only ever inserted, never updated or deleted.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "radiology_report_history")
public class RadiologyReportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "report_version", nullable = false)
    private Long reportVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RadiologyReportStatus status;

    @Column(name = "clinical_history", columnDefinition = "TEXT")
    private String clinicalHistory;

    @Column(name = "technique", columnDefinition = "TEXT")
    private String technique;

    @Column(name = "comparison", columnDefinition = "TEXT")
    private String comparison;

    @Column(name = "findings_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode findingsJson;

    @Column(name = "impression", columnDefinition = "TEXT")
    private String impression;

    @Column(name = "signatories_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode signatoriesJson;

    @Column(name = "change_reason", columnDefinition = "TEXT", nullable = false)
    private String changeReason;

    @Column(name = "changed_by")
    private String changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private Instant changedAt;
}
