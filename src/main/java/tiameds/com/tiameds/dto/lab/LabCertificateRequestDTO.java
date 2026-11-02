package tiameds.com.tiameds.dto.lab;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LabCertificateRequestDTO {

    private Long id;
    private Long labId;
    private String labName;

    // Same value as the multipart part name of the documents endpoint, e.g. nablAccreditationCertificate
    private String documentType;

    private String requestedBy;
    private String newFileUrl;
    private String previousFileUrl;
    private LocalDate issuingDate;
    private LocalDate expiryDate;

    // PENDING, APPROVED or REJECTED
    private String status;
    private String rejectionReason;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
}
