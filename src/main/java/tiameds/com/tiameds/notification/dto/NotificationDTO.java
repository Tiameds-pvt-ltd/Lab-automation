package tiameds.com.tiameds.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.notification.entity.Notification;
import tiameds.com.tiameds.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDTO {
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    // Only filled for REPORT_UPDATED / VISIT_SAMPLE_CHANGED (read back from the stored details)
    private String labName;
    private String patientName;
    private Long reportId;

    // Only filled for VISIT_SAMPLE_CHANGED
    private String sampleAction;          // UPDATED / DELETED
    private List<String> samples;
    private JsonNode patientDetails;      // patientId, name, gender, age, dateOfBirth, phone, email

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
        if ((n.getType() == NotificationType.REPORT_UPDATED || n.getType() == NotificationType.VISIT_SAMPLE_CHANGED)
                && n.getPayloadJson() != null) {
            try {
                JsonNode details = MAPPER.readTree(n.getPayloadJson());
                dto.labName = details.hasNonNull("labName") ? details.get("labName").asText() : null;
                dto.patientName = details.hasNonNull("patientName") ? details.get("patientName").asText() : null;
                dto.reportId = details.hasNonNull("reportId") ? details.get("reportId").asLong() : null;
                if (n.getType() == NotificationType.VISIT_SAMPLE_CHANGED) {
                    dto.sampleAction = details.hasNonNull("sampleAction") ? details.get("sampleAction").asText() : null;
                    if (details.hasNonNull("samples") && details.get("samples").isArray()) {
                        List<String> names = new java.util.ArrayList<>();
                        details.get("samples").forEach(s -> names.add(s.asText()));
                        dto.samples = names;
                    }
                    dto.patientDetails = details.hasNonNull("patient") ? details.get("patient") : null;
                }
            } catch (Exception ignored) {
                // details are optional; the title / message still tell the story
            }
        }
        return dto;
    }
}
