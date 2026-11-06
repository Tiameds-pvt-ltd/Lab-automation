package tiameds.com.tiameds.notification.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.notification.service.NotificationException;
import tiameds.com.tiameds.notification.service.NotificationService;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.Optional;

/**
 * Holds patient edits made by non-admin users (DESKROLE, TECHNICIAN) for admin approval, WITHOUT changing
 * PatientController: the existing endpoints are intercepted here and, instead of updating the patient, the
 * proposed values are stored as a PENDING notification for the admins of the lab.
 *
 * ADMIN / SUPERADMIN edits are not intercepted and apply immediately, as before.
 * Switch off with notification.patient-edit-approval.enabled=false (no redeploy of code needed, just config).
 *
 * Runs first (highest precedence), so an intercepted call is neither audited as an update by AuditAspect nor
 * executed by the controller; the real update is audited when the admin approves.
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "notification.patient-edit-approval", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class PatientEditApprovalAspect {

    private final NotificationService notificationService;
    private final UserService userService;

    public PatientEditApprovalAspect(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    // Both existing patient update endpoints: PUT /lab/{labId}/update-patient-details/{patientId}
    // and PUT /lab/{labId}/update-patient/{patientId}. Arguments: (Long labId, Long patientId, PatientDTO body)
    @Around("execution(public * tiameds.com.tiameds.controller.lab.PatientController.updatePatientDetails(..)) "
            + "|| execution(public * tiameds.com.tiameds.controller.lab.PatientController.updatePatient(..))")
    public Object holdPatientEditForApproval(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        Optional<User> userOpt = getAuthenticatedUser();

        // Not logged in / unexpected shape / admin: let the controller handle it exactly as before
        if (userOpt.isEmpty() || args.length < 3
                || !(args[0] instanceof Long labId)
                || !(args[1] instanceof Long patientId)
                || !(args[2] instanceof PatientDTO patientDTO)
                || !notificationService.requiresAdminApproval(userOpt.get())) {
            return pjp.proceed();
        }

        User user = userOpt.get();
        try {
            Object result = notificationService.submitPatientEditRequest(user, labId, patientId, patientDTO);
            log.info("Patient {} edit by {} held for admin approval (lab {})", patientId, user.getUsername(), labId);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Your changes were sent to the admin for approval. The patient will be updated once it is approved.",
                    HttpStatus.ACCEPTED,
                    result);
        } catch (NotificationException e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), e.getStatus());
        }
    }

    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof MyUserDetails myUserDetails) {
            return Optional.of(myUserDetails.getUser());
        }
        if (principal instanceof UserDetails userDetails) {
            return userService.findByUsername(userDetails.getUsername());
        }
        return Optional.empty();
    }
}
