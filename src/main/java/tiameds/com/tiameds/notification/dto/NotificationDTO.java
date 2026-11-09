package tiameds.com.tiameds.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.notification.entity.Notification;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDTO {
    private Long id;
    private Long labId;
    private String type;
    private String entityType;
    private Long entityId;
    private Long visitId;
    private String visitCode;
    private String title;
    private String message;
    private String status;
    private String targetRole;
    private Long requestedByUserId;
    private String requestedByUsername;
    private Long reviewedByUserId;
    private String reviewedByUsername;
    private LocalDateTime reviewedAt;
    private String rejectionReason;
    private String approvalReason;
    private LocalDateTime createdAt;

    // Only filled by the detail endpoint, for PATIENT_EDIT_REQUEST
    private PatientDTO currentPatient;
    private PatientDTO proposedPatient;
    private List<String> changedFields;

    // Only filled by the detail endpoint, for TEST_CHANGE_REQUEST / TEST_REFERENCE_CHANGE_REQUEST
    private CatalogChangePayload proposedChange;
    private Object currentValue;

    public static NotificationDTO from(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.id = n.getId();
        dto.labId = n.getLabId();
        dto.type = n.getType() != null ? n.getType().name() : null;
        dto.entityType = n.getEntityType();
        dto.entityId = n.getEntityId();
        dto.visitId = n.getVisitId();
        dto.visitCode = n.getVisitCode();
        dto.title = n.getTitle();
        dto.message = n.getMessage();
        dto.status = n.getStatus() != null ? n.getStatus().name() : null;
        dto.targetRole = n.getTargetRole();
        dto.requestedByUserId = n.getRequestedByUserId();
        dto.requestedByUsername = n.getRequestedByUsername();
        dto.reviewedByUserId = n.getReviewedByUserId();
        dto.reviewedByUsername = n.getReviewedByUsername();
        dto.reviewedAt = n.getReviewedAt();
        dto.rejectionReason = n.getRejectionReason();
        dto.approvalReason = n.getReviewRemark();
        dto.createdAt = n.getCreatedAt();
        return dto;
    }
}
