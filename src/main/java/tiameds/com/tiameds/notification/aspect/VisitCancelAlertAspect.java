package tiameds.com.tiameds.notification.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import tiameds.com.tiameds.dto.lab.CancellationDataDTO;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.notification.service.NotificationService;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;

import java.util.Optional;

/**
 * Tells the lab's admins when a non-admin user (DESKROLE, TECHNICIAN) cancels a visit, WITHOUT changing
 * PatientController: PUT /lab/{labId}/visit/{visitId}/cancel runs exactly as before and, once it has succeeded,
 * an information-only VISIT_CANCELLED notification is stored for the admins.
 *
 * ADMIN / SUPERADMIN cancellations raise no alert. A problem while raising the alert is only logged and can never
 * affect the cancellation. Switch off with notification.visit-cancel-alert.enabled=false.
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(prefix = "notification.visit-cancel-alert", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class VisitCancelAlertAspect {

    private final NotificationService notificationService;
    private final UserService userService;

    public VisitCancelAlertAspect(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    // Arguments: (Long labId, Long visitId, CancellationDataDTO body, HttpServletRequest request)
    @AfterReturning(
            pointcut = "execution(public * tiameds.com.tiameds.controller.lab.PatientController.cancelVisit(..))",
            returning = "response")
    public void alertAdminsOfCancelledVisit(JoinPoint jp, Object response) {
        try {
            // the controller returns an error response (not an exception) when the cancel failed
            if (!(response instanceof ResponseEntity<?> entity) || !entity.getStatusCode().is2xxSuccessful()) {
                return;
            }
            Object[] args = jp.getArgs();
            if (args.length < 3 || !(args[0] instanceof Long labId) || !(args[1] instanceof Long visitId)) {
                return;
            }
            Optional<User> userOpt = getAuthenticatedUser();
            if (userOpt.isEmpty() || !notificationService.requiresAdminApproval(userOpt.get())) {
                return;
            }
            String reason = args[2] instanceof CancellationDataDTO dto ? dto.getVisitCancellationReason() : null;
            notificationService.raiseVisitCancelledAlert(userOpt.get(), labId, visitId, reason);
        } catch (Exception e) {
            log.warn("Could not raise the visit-cancelled alert: {}", e.getMessage(), e);
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
