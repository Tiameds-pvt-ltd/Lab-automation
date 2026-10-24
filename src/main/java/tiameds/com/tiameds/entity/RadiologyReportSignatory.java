package tiameds.com.tiameds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * A doctor who signs a radiology report. Name/qualification/licence are copied from the
 * doctor at save time so a signed report does not change when the profile is edited.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "radiology_report_signatory")
public class RadiologyReportSignatory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    // REPORTING or VERIFYING
    @Column(name = "role", nullable = false, length = 20)
    private String role = "REPORTING";

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "doctor_name", nullable = false)
    private String doctorName;

    @Column(name = "designation")
    private String designation;

    @Column(name = "qualification")
    private String qualification;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
