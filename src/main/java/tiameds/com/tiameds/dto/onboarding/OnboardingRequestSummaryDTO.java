package tiameds.com.tiameds.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * An onboarding request (a lab awaiting or having received platform review), as shown to the reviewer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingRequestSummaryDTO {
    private Long labId;
    private String labName;
    private String labType;
    private String labEmail;
    private String labPhone;
    private String city;
    private String state;
    private String labServices;

    private String organizationName;
    private String organizationType;
    private String organizationPan;
    private String organizationGstin;

    private String licenseNumber;
    private String establishmentRegistrationNumber;
    private String issuingAuthority;
    private LocalDate issuingDate;
    private LocalDate expiryDate;

    private String labLogo;
    private String establishmentRegistrationCertificate;
    private String organizationGstinCertificate;
    private String gstinCertificate;
    private String biomedicalWasteAuthorizationCertificate;
    private String nablAccreditationCertificate;

    private Long applicantUserId;
    private String applicantName;
    private String applicantEmail;

    private String approvalStatus;
    private LocalDateTime approvalRequestedAt;
    private LocalDateTime reviewedAt;
    private Long reviewedBy;
    private String rejectionReason;
}
