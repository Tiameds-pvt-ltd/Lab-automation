package tiameds.com.tiameds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "lab_report")
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Column(name = "visit_id", nullable = false)
    private Long visitId;

    @Column(name = "test_name", nullable = false)
    private String testName;

    @Column(name = "test_category", nullable = false)
    private String testCategory;

    @Column(name = "patient_name")
    private String patientName; // Added this field if required

    @Column(name = "lab_id", nullable = false)
    private Long labId;

    @Column(name = "reference_description")
    private String referenceDescription;

    @Column(name = "reference_range")
    private String referenceRange;

    @Column(name = "reference_age_range")
    private String referenceAgeRange;

    @Column(name = "entered_value")
    private String enteredValue;

    @Column(name = "unit")
    private String unit;

    // new fields added
    @Column(name = "description",length = 500)
    private String description;

    @Column(name = "remarks" , length = 300)
    private String remarks;

    @Column(name= "comment", length = 500)
    private String comments;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // new -field store json
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String reportJson;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String referenceRanges;

    @Column(name = "test_rows", columnDefinition = "jsonb" ,nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<TestRow> testRows;

    @Column(name = "report_code", unique = true)
    private String reportCode;

    @Transient
    private String patientCode;

    @Transient
    private String visitCode;

    @Transient
    private String createdDateTime;
}

