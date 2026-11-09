package tiameds.com.tiameds.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.audit.AuditLogService;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.LabAuditLogs;
import tiameds.com.tiameds.entity.PatientEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VisitEntity;
import tiameds.com.tiameds.notification.dto.NotificationDTO;
import tiameds.com.tiameds.notification.entity.Notification;
import tiameds.com.tiameds.notification.entity.NotificationStatus;
import tiameds.com.tiameds.notification.entity.NotificationType;
import tiameds.com.tiameds.notification.repository.NotificationRepository;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.PatientRepository;
import tiameds.com.tiameds.repository.VisitRepository;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.UpdatePatientService;
import tiameds.com.tiameds.utils.LabAccessableFilter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Notifications / approval requests. Self-contained: it only calls existing services (it never changes them).
 *
 * Patient edit flow:
 *  1. a lab member submits the proposed PatientDTO  -> stored as a PENDING notification, the patient is untouched
 *  2. an ADMIN of that lab approves -> the stored values are applied through the existing
 *     UpdatePatientService.updatePatientDetails (same logic as /lab/{labId}/update-patient-details/{patientId})
 *     or rejects -> nothing is applied, the reason is kept for the requester.
 */
@Slf4j
@Service
@Transactional
public class NotificationService {

    private static final String ENTITY_PATIENT = "PATIENT";
    private static final String ROLE_SUPERADMIN = "SUPERADMIN";

    private final NotificationRepository notificationRepository;
    private final LabRepository labRepository;
    private final PatientRepository patientRepository;
    private final UserService userService;
    private final LabAccessableFilter labAccessableFilter;
    private final UpdatePatientService updatePatientService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final PatientChangeDescriber changeDescriber;
    private final VisitRepository visitRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               LabRepository labRepository,
                               PatientRepository patientRepository,
                               UserService userService,
                               LabAccessableFilter labAccessableFilter,
                               UpdatePatientService updatePatientService,
                               AuditLogService auditLogService,
                               ObjectMapper objectMapper,
                               PatientChangeDescriber changeDescriber,
                               VisitRepository visitRepository) {
        this.changeDescriber = changeDescriber;
        this.visitRepository = visitRepository;
        this.notificationRepository = notificationRepository;
        this.labRepository = labRepository;
        this.patientRepository = patientRepository;
        this.userService = userService;
        this.labAccessableFilter = labAccessableFilter;
        this.updatePatientService = updatePatientService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------ submit

    /** Stores a patient edit as a PENDING request for the lab's admins. The patient is NOT changed here. */
    public NotificationDTO submitPatientEditRequest(User user, Long labId, Long patientId, PatientDTO proposed) {
        if (proposed == null) {
            throw new NotificationException(HttpStatus.BAD_REQUEST, "Patient details are required");
        }
        Lab lab = requireAccessibleLabMember(user, labId);
        PatientEntity patient = requirePatientOfLab(patientId, lab);

        if (notificationRepository.existsByLabIdAndTypeAndEntityIdAndStatus(
                labId, NotificationType.PATIENT_EDIT_REQUEST, patientId, NotificationStatus.PENDING)) {
            throw new NotificationException(HttpStatus.CONFLICT,
                    "An edit request for this patient is already waiting for admin approval");
        }

        List<PatientChangeDescriber.Change> changes = changeDescriber.describe(new PatientDTO(patient), proposed);
        if (changes.isEmpty()) {
            throw new NotificationException(HttpStatus.BAD_REQUEST, "No changes detected in the patient details");
        }
        Long visitId = proposed.getVisit() != null ? proposed.getVisit().getVisitId() : null;

        Notification n = new Notification();
        n.setLabId(labId);
        n.setType(NotificationType.PATIENT_EDIT_REQUEST);
        n.setEntityType(ENTITY_PATIENT);
        n.setEntityId(patientId);
        n.setTitle("Patient edit request: " + patientName(patient));
        n.setVisitId(visitId);
        n.setVisitCode(visitCodeOf(visitId));
        n.setMessage(user.getUsername() + " requested changes: "
                + changes.stream().map(PatientChangeDescriber.Change::text).collect(Collectors.joining("; ")));
        n.setPayloadJson(toJson(proposed));
        n.setRequestedByUserId(user.getId());
        n.setRequestedByUsername(user.getUsername());
        n.setStatus(NotificationStatus.PENDING);

        Notification saved = notificationRepository.save(n);
        log.info("Patient edit request {} submitted for patient {} in lab {} by {}",
                saved.getId(), patientId, labId, user.getUsername());
        return NotificationDTO.from(saved);
    }

    // ------------------------------------------------------------------ visit cancelled alert

    /**
     * Raises an information-only alert for the lab's admins after a non-admin cancelled a visit.
     * The cancellation itself has already happened; this only tells the admins. Returns null if nothing was created.
     */
    public NotificationDTO raiseVisitCancelledAlert(User user, Long labId, Long visitId, String reason) {
        if (notificationRepository.existsByLabIdAndTypeAndEntityIdAndStatus(
                labId, NotificationType.VISIT_CANCELLED, visitId, NotificationStatus.PENDING)) {
            return null; // an unseen alert for this visit already exists
        }
        VisitEntity visit = visitRepository.findById(visitId).orElse(null);
        String visitCode = visit != null ? visit.getVisitCode() : null;
        String patient = visit != null && visit.getPatient() != null ? patientName(visit.getPatient()) : null;

        Notification n = new Notification();
        n.setLabId(labId);
        n.setType(NotificationType.VISIT_CANCELLED);
        n.setEntityType("VISIT");
        n.setEntityId(visitId);
        n.setVisitId(visitId);
        n.setVisitCode(visitCode);
        n.setTitle("Visit cancelled" + (visitCode != null ? ": " + visitCode : ""));
        n.setMessage(user.getUsername() + " cancelled the visit"
                + (patient != null ? " of " + patient : "")
                + (reason != null && !reason.isBlank() ? ". Reason: " + reason.trim() : ""));
        n.setRequestedByUserId(user.getId());
        n.setRequestedByUsername(user.getUsername());
        n.setStatus(NotificationStatus.PENDING);

        Notification saved = notificationRepository.save(n);
        log.info("Visit {} cancelled by {} in lab {}: admin alert {} raised",
                visitId, user.getUsername(), labId, saved.getId());
        return NotificationDTO.from(saved);
    }

    /**
     * Raises an information-only alert after a test / test reference was changed.
     * Audience, decided from who made the change:
     *  - TECHNICIAN / DESKROLE -> the lab's admins (and super admins)
     *  - ADMIN                 -> super admins only
     *  - SUPERADMIN            -> nobody (returns null)
     * entity_id is left empty on purpose: every change is its own alert (the one-pending-per-entity rule only
     * applies to rows that have an entity id).
     *
     * @param title      e.g. "Test reference updated"
     * @param actionText e.g. "updated test reference" (the message reads "<user> <actionText>: <name> (<code>)")
     */
    public NotificationDTO raiseChangeAlert(User user, Long labId, NotificationType type, String entityType,
                                            String title, String actionText, String name, String code) {
        if (isSuperAdmin(user)) {
            return null;
        }
        String what = (name != null && !name.isBlank() ? name.trim() : null);
        if (code != null && !code.isBlank()) {
            what = (what != null ? what + " " : "") + "(" + code.trim() + ")";
        }
        boolean byAdmin = isAdmin(user);

        Notification n = new Notification();
        n.setLabId(labId);
        n.setType(type);
        n.setEntityType(entityType);
        n.setTitle(title);
        n.setMessage(user.getUsername() + (byAdmin ? " (admin) " : " ") + actionText + (what != null ? ": " + what : ""));
        n.setTargetRole(byAdmin ? ROLE_SUPERADMIN : null);
        n.setRequestedByUserId(user.getId());
        n.setRequestedByUsername(user.getUsername());
        n.setStatus(NotificationStatus.PENDING);
        Notification saved = notificationRepository.save(n);
        log.info("{} by {} in lab {}: alert {} raised for {}", title, user.getUsername(), labId, saved.getId(),
                byAdmin ? "super admins" : "admins");
        return NotificationDTO.from(saved);
    }

    /** An admin has seen an information-only alert; it leaves the pending count. */
    public NotificationDTO acknowledge(User admin, Long labId, Long notificationId) {
        requireAdminOfLab(admin, labId);
        Notification n = notificationRepository.findByIdForUpdate(notificationId)
                .filter(row -> Objects.equals(row.getLabId(), labId))
                .orElseThrow(() -> new NotificationException(HttpStatus.NOT_FOUND, "Notification not found"));
        return acknowledge(admin, n);
    }

    /** Super admin: acknowledge an alert of any lab (no lab membership needed). */
    public NotificationDTO acknowledgeAsSuperAdmin(User superAdmin, Long notificationId) {
        requireSuperAdmin(superAdmin);
        Notification n = notificationRepository.findByIdForUpdate(notificationId)
                .orElseThrow(() -> new NotificationException(HttpStatus.NOT_FOUND, "Notification not found"));
        return acknowledge(superAdmin, n);
    }

    private NotificationDTO acknowledge(User reviewer, Notification n) {
        if (n.getType() == NotificationType.PATIENT_EDIT_REQUEST) {
            throw new NotificationException(HttpStatus.BAD_REQUEST,
                    "This notification must be approved or rejected, not acknowledged");
        }
        if (ROLE_SUPERADMIN.equals(n.getTargetRole()) && !isSuperAdmin(reviewer)) {
            throw new NotificationException(HttpStatus.FORBIDDEN, "This alert is only for super admins");
        }
        if (n.getStatus() != NotificationStatus.PENDING) {
            throw new NotificationException(HttpStatus.CONFLICT, "This alert was already acknowledged");
        }
        n.setStatus(NotificationStatus.ACKNOWLEDGED);
        stampReview(n, reviewer);
        return NotificationDTO.from(notificationRepository.save(n));
    }

    // ------------------------------------------------------------------ read

    /**
     * Admin view: every notification of the lab, optionally filtered by status.
     * Alerts reserved for super admins (an ADMIN changed something) are hidden from plain admins.
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getLabNotifications(User user, Long labId, NotificationStatus status) {
        requireAdminOfLab(user, labId);
        boolean superAdmin = isSuperAdmin(user);
        List<Notification> rows = status == null
                ? notificationRepository.findByLabIdOrderByCreatedAtDesc(labId)
                : notificationRepository.findByLabIdAndStatusOrderByCreatedAtDesc(labId, status);
        return rows.stream()
                .filter(n -> superAdmin || !ROLE_SUPERADMIN.equals(n.getTargetRole()))
                .map(NotificationDTO::from).collect(Collectors.toList());
    }

    /** Super admin view across all labs: the alerts raised because an ADMIN changed something. */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getSuperAdminNotifications(User user, NotificationStatus status) {
        requireSuperAdmin(user);
        List<Notification> rows = status == null
                ? notificationRepository.findByTargetRoleOrderByCreatedAtDesc(ROLE_SUPERADMIN)
                : notificationRepository.findByTargetRoleAndStatusOrderByCreatedAtDesc(ROLE_SUPERADMIN, status);
        return rows.stream().map(NotificationDTO::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getSuperAdminPendingCount(User user) {
        requireSuperAdmin(user);
        return notificationRepository.countByTargetRoleAndStatus(ROLE_SUPERADMIN, NotificationStatus.PENDING);
    }

    /** The requests the current user raised themselves, so they can see approved / rejected (with reason). */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyRequests(User user, Long labId) {
        requireLabMember(user, labId);
        return notificationRepository.findByLabIdAndRequestedByUserIdOrderByCreatedAtDesc(labId, user.getId())
                .stream().map(NotificationDTO::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getPendingCount(User user, Long labId) {
        requireAdminOfLab(user, labId);
        return isSuperAdmin(user)
                ? notificationRepository.countByLabIdAndStatus(labId, NotificationStatus.PENDING)
                : notificationRepository.countVisibleToAdmins(labId, NotificationStatus.PENDING);
    }

    /** Detail with the current and the proposed patient values side by side. Admins, or the requester. */
    @Transactional(readOnly = true)
    public NotificationDTO getNotification(User user, Long labId, Long notificationId) {
        requireLabMember(user, labId);
        Notification n = requireNotificationOfLab(notificationId, labId);
        if (!isAdmin(user) && !Objects.equals(n.getRequestedByUserId(), user.getId())) {
            throw new NotificationException(HttpStatus.FORBIDDEN, "You are not allowed to view this notification");
        }
        if (ROLE_SUPERADMIN.equals(n.getTargetRole()) && !isSuperAdmin(user)
                && !Objects.equals(n.getRequestedByUserId(), user.getId())) {
            throw new NotificationException(HttpStatus.FORBIDDEN, "This alert is only for super admins");
        }

        NotificationDTO dto = NotificationDTO.from(n);
        if (n.getType() == NotificationType.PATIENT_EDIT_REQUEST && n.getPayloadJson() != null) {
            PatientDTO proposed = fromJson(n.getPayloadJson());
            dto.setProposedPatient(proposed);
            if (dto.getVisitId() == null && proposed.getVisit() != null) {
                // requests raised before visit codes were stored
                dto.setVisitId(proposed.getVisit().getVisitId());
                dto.setVisitCode(visitCodeOf(dto.getVisitId()));
            }
            patientRepository.findById(n.getEntityId()).ifPresent(patient -> {
                PatientDTO current = new PatientDTO(patient);
                dto.setCurrentPatient(current);
                dto.setChangedFields(changeDescriber.describe(current, proposed).stream()
                        .map(PatientChangeDescriber.Change::field).collect(Collectors.toList()));
            });
        }
        return dto;
    }

    // ------------------------------------------------------------------ review

    /** Applies the stored patient edit through the existing update logic and marks the request APPROVED. */
    public NotificationDTO approve(User reviewer, Long labId, Long notificationId, String remark) {
        Lab lab = requireAdminOfLab(reviewer, labId);
        if (remark == null || remark.isBlank()) {
            throw new NotificationException(HttpStatus.BAD_REQUEST, "An approval reason is required");
        }
        Notification n = requirePendingForUpdate(notificationId, labId);

        PatientEntity patient = patientRepository.findById(n.getEntityId())
                .filter(p -> p.getLabs().contains(lab))
                .orElseThrow(() -> new NotificationException(HttpStatus.NOT_FOUND,
                        "Patient no longer exists in this lab; reject this request instead"));

        PatientDTO proposed = fromJson(n.getPayloadJson());
        String before = toJson(new PatientDTO(patient));

        // Same code path as the existing endpoint; the requester is recorded as the editor
        PatientDTO updated = updatePatientService.updatePatientDetails(
                patient, lab, proposed, n.getRequestedByUsername());

        n.setStatus(NotificationStatus.APPROVED);
        if (remark != null && !remark.isBlank()) {
            n.setReviewRemark(remark.trim());
        }
        stampReview(n, reviewer);
        Notification saved = notificationRepository.save(n);

        writeAudit(reviewer, labId, n, "UPDATE", before, toJson(updated),
                "Approved patient edit request #" + n.getId() + " raised by " + n.getRequestedByUsername()
                        + (n.getReviewRemark() != null ? ": " + n.getReviewRemark() : ""),
                LabAuditLogs.Severity.MEDIUM);
        log.info("Patient edit request {} approved by {}", n.getId(), reviewer.getUsername());
        return NotificationDTO.from(saved);
    }

    /** Marks the request REJECTED; the patient stays exactly as it was. */
    public NotificationDTO reject(User reviewer, Long labId, Long notificationId, String reason) {
        requireAdminOfLab(reviewer, labId);
        if (reason == null || reason.isBlank()) {
            throw new NotificationException(HttpStatus.BAD_REQUEST, "A rejection reason is required");
        }
        Notification n = requirePendingForUpdate(notificationId, labId);

        n.setStatus(NotificationStatus.REJECTED);
        n.setRejectionReason(reason.trim());
        stampReview(n, reviewer);
        Notification saved = notificationRepository.save(n);

        writeAudit(reviewer, labId, n, "UPDATE_REJECTED", null, n.getPayloadJson(),
                "Rejected patient edit request #" + n.getId() + " raised by " + n.getRequestedByUsername()
                        + ": " + n.getRejectionReason(),
                LabAuditLogs.Severity.LOW);
        log.info("Patient edit request {} rejected by {}", n.getId(), reviewer.getUsername());
        return NotificationDTO.from(saved);
    }

    // ------------------------------------------------------------------ helpers

    private Lab requireAccessibleLabMember(User user, Long labId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new NotificationException(HttpStatus.NOT_FOUND, "Lab not found"));
        if (!labAccessableFilter.isLabAccessible(lab)) {
            throw new NotificationException(HttpStatus.UNAUTHORIZED, "Lab is not accessible");
        }
        if (!userService.isUserMemberOfLab(user.getId(), labId)) {
            throw new NotificationException(HttpStatus.UNAUTHORIZED, "User is not a member of this lab");
        }
        return lab;
    }

    private void requireLabMember(User user, Long labId) {
        if (!userService.isUserMemberOfLab(user.getId(), labId)) {
            throw new NotificationException(HttpStatus.UNAUTHORIZED, "User is not a member of this lab");
        }
    }

    /** Reviewer = ADMIN (or SUPERADMIN) who is a member of the lab the request belongs to. */
    private Lab requireAdminOfLab(User user, Long labId) {
        Lab lab = requireAccessibleLabMember(user, labId);
        if (!isAdmin(user)) {
            throw new NotificationException(HttpStatus.FORBIDDEN,
                    "Only an ADMIN of this lab can review notifications");
        }
        return lab;
    }

    /** True for users who may not change a patient directly: anyone who is neither ADMIN nor SUPERADMIN. */
    public boolean requiresAdminApproval(User user) {
        return !isAdmin(user);
    }

    private boolean isAdmin(User user) {
        Set<String> roles = roleNames(user);
        return roles.contains("ADMIN") || roles.contains(ROLE_SUPERADMIN);
    }

    public boolean isSuperAdmin(User user) {
        return roleNames(user).contains(ROLE_SUPERADMIN);
    }

    private void requireSuperAdmin(User user) {
        if (!isSuperAdmin(user)) {
            throw new NotificationException(HttpStatus.FORBIDDEN, "Only a SUPERADMIN can use this");
        }
    }

    private Set<String> roleNames(User user) {
        if (user.getRoles() == null) {
            return Set.of();
        }
        return user.getRoles().stream()
                .map(r -> r.getName())
                .filter(Objects::nonNull)
                .map(name -> name.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private PatientEntity requirePatientOfLab(Long patientId, Lab lab) {
        return patientRepository.findById(patientId)
                .filter(p -> p.getLabs().contains(lab))
                .orElseThrow(() -> new NotificationException(HttpStatus.NOT_FOUND,
                        "Patient not found for the specified lab"));
    }

    private Notification requireNotificationOfLab(Long notificationId, Long labId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> Objects.equals(n.getLabId(), labId))
                .orElseThrow(() -> new NotificationException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    private Notification requirePendingForUpdate(Long notificationId, Long labId) {
        Notification n = notificationRepository.findByIdForUpdate(notificationId)
                .filter(row -> Objects.equals(row.getLabId(), labId))
                .orElseThrow(() -> new NotificationException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (n.getStatus() != NotificationStatus.PENDING) {
            throw new NotificationException(HttpStatus.CONFLICT,
                    "This request was already " + n.getStatus().name().toLowerCase(Locale.ROOT));
        }
        if (n.getType() != NotificationType.PATIENT_EDIT_REQUEST) {
            throw new NotificationException(HttpStatus.BAD_REQUEST, "This notification type cannot be reviewed");
        }
        return n;
    }

    private void stampReview(Notification n, User reviewer) {
        n.setReviewedByUserId(reviewer.getId());
        n.setReviewedByUsername(reviewer.getUsername());
        n.setReviewedAt(LocalDateTime.now());
    }

    private String visitCodeOf(Long visitId) {
        if (visitId == null) {
            return null;
        }
        return visitRepository.findById(visitId).map(VisitEntity::getVisitCode).orElse(null);
    }

    private String patientName(PatientEntity patient) {
        String first = patient.getFirstName() != null ? patient.getFirstName() : "";
        String last = patient.getLastName() != null ? patient.getLastName() : "";
        String name = (first + " " + last).trim();
        return name.isEmpty() ? "patient #" + patient.getPatientId() : name;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new NotificationException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not serialize the request: " + e.getMessage());
        }
    }

    private PatientDTO fromJson(String json) {
        try {
            return objectMapper.readValue(json, PatientDTO.class);
        } catch (Exception e) {
            throw new NotificationException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stored request could not be read: " + e.getMessage());
        }
    }

    private void writeAudit(User actor, Long labId, Notification n, String action,
                            String oldValue, String newValue, String reason, LabAuditLogs.Severity severity) {
        try {
            LabAuditLogs audit = new LabAuditLogs();
            audit.setTimestamp(LocalDateTime.now());
            audit.setUserId(actor.getId());
            audit.setUsername(actor.getUsername());
            audit.setLab_id(String.valueOf(labId));
            audit.setModule("Lab");
            audit.setEntityType("Patient");
            audit.setEntityId(String.valueOf(n.getEntityId()));
            audit.setActionType(action);
            audit.setOldValue(oldValue);
            audit.setNewValue(newValue);
            audit.setChangeReason(reason);
            audit.setSeverity(severity);
            if (actor.getRoles() != null && !actor.getRoles().isEmpty()) {
                audit.setRole(actor.getRoles().iterator().next().getName());
            }
            auditLogService.persistAsync(audit);
        } catch (Exception e) {
            // An audit problem must never undo the review itself
            log.warn("Could not write audit log for notification {}: {}", n.getId(), e.getMessage());
        }
    }
}
