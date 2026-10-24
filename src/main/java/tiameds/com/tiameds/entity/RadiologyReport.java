package tiameds.com.tiameds.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * One radiology report per (lab, visit, test). The structured body lives in findingsJson,
 * the part that is queried/searched most (impression) is a real column.
 * Separate from ReportEntity (lab_report), which keeps serving pathology results.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "radiology_report")
public class RadiologyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "report_code", nullable = false, unique = true)
    private String reportCode;

    @Column(name = "visit_id", nullable = false)
    private Long visitId;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "lab_id", nullable = false)
    private Long labId;

    // The exact template version the report was written against (null = free-form report).
    @Column(name = "template_id")
    private Long templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RadiologyReportStatus status = RadiologyReportStatus.DRAFT;

    @Column(name = "clinical_history", columnDefinition = "TEXT")
    private String clinicalHistory;

    @Column(name = "technique", columnDefinition = "TEXT")
    private String technique;

    @Column(name = "comparison", columnDefinition = "TEXT")
    private String comparison;

    @Column(name = "findings_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode findingsJson;

    @Column(name = "impression", columnDefinition = "TEXT")
    private String impression;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "finalized_by")
    private String finalizedBy;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Optimistic locking: two users saving the same report cannot overwrite each other.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
