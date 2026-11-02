package tiameds.com.tiameds.dto.lab;

import lombok.Builder;
import lombok.Data;
import tiameds.com.tiameds.dto.auth.UserProfileDTO;

import java.util.List;
import java.util.Map;

/**
 * Everything the profile screen needs in one response: the user's own details and, for each lab the user belongs to
 * (or the one requested), the lab / organization details, the current document URLs and the certificate requests.
 */
@Data
@Builder
public class LabProfileDTO {

    private UserProfileDTO user;
    private List<LabSection> labs;

    @Data
    @Builder
    public static class LabSection {
        private Long labId;
        private String labName;
        private Boolean isActive;
        private String approvalStatus;

        // Organization and lab details, same shape as GET /lab/admin/{labId}/onboarding-details
        private LabOnboardingDetailsDTO details;

        // Live logo / certificate URLs (null if not uploaded), same as GET /lab/admin/{labId}/documents
        private Map<String, String> documents;

        // Newest first: pending, rejected (with reason) and approved requests, same as GET .../certificate-requests
        private List<LabCertificateRequestDTO> certificateRequests;
    }
}
