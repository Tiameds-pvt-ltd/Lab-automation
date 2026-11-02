package tiameds.com.tiameds.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A request to add or replace one lab certificate. The lab keeps its current certificate until a platform admin
 * approves the request; no file is ever deleted from S3, so every version stays available as history.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lab_certificate_request")
public class LabCertificateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lab_id", nullable = false)
    private Long labId;

    // LabDocumentType name, e.g. NABL_ACCREDITATION_CERTIFICATE
    @Column(name = "document_type", nullable = false, length = 60)
    private String documentType;

    @Column(name = "requested_by_user_id")
    private Long requestedByUserId;

    @Column(name = "new_file_url", nullable = false, columnDefinition = "TEXT")
    private String newFileUrl;

    // The certificate that was live when the request was approved; null until then
    @Column(name = "previous_file_url", columnDefinition = "TEXT")
    private String previousFileUrl;

    // Only for the establishment registration certificate
    @Column(name = "issuing_date")
    private LocalDate issuingDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LabApprovalStatus status = LabApprovalStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
