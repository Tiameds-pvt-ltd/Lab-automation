package tiameds.com.tiameds.notification.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import tiameds.com.tiameds.dto.lab.PatientVisitSampleDto;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.notification.service.NotificationService;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;

import java.util.Optional;

/**
 * Tells the lab's admins when a non-admin user (DESKROLE, TECHNICIAN) updates or deletes a visit's samples AFTER the
 * results were entered, WITHOUT changing PatientVisitSample: PUT /lab/update-samples and DELETE /lab/delete-samples
 * run exactly as before and, once they have succeeded, an information-only VISIT_SAMPLE_CHANGED notification
 * (patient, visit, lab and sample details, no approve / reject) is stored for the admins.
 *
 * Nothing is raised when the visit has no results yet, or when an ADMIN / SUPERADMIN made the change. A problem while
 * raising the alert is only logged and can never affect the sample change. Switch off with
 * notification.visit-sample-change-alert.enabled=false.
 *
 * Runs outside the controller's @Transactional, so the sample change is already committed when the alert is stored.
 */
@Slf4j
@Aspect
@Component
// +10: an @AfterReturning with "returning" needs ExposeInvocationInterceptor (HIGHEST_PRECEDENCE) to run first;
// still far outside the controller's @Transactional.
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(prefix = "notification.visit-sample-change-alert", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class VisitSampleChangeAlertAspect {

    private final NotificationService notificationService;
    private final UserService userService;

    public VisitSampleChangeAlertAspect(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    // Arguments: (PatientVisitSampleDto request, HttpServletRequest httpRequest)
    @AfterReturning(
            pointcut = "execution(public * tiameds.com.tiameds.controller.lab.PatientVisitSample.updateSampleWithPatientVisit(..))",
            returning = "response")
    public void alertOnUpdate(JoinPoint jp, Object response) {
        raise(jp, response, NotificationService.OP_UPDATE);
    }

    @AfterReturning(
            pointcut = "execution(public * tiameds.com.tiameds.controller.lab.PatientVisitSample.deleteSampleWithPatientVisit(..))",
            returning = "response")
    public void alertOnDelete(JoinPoint jp, Object response) {
        raise(jp, response, NotificationService.OP_DELETE);
    }

    private void raise(JoinPoint jp, Object response, String action) {
        try {
            // the controller returns error responses (not exceptions) when the change failed
            if (!(response instanceof ResponseEntity<?> entity) || !entity.getStatusCode().is2xxSuccessful()) {
                return;
            }
            Object[] args = jp.getArgs();
            if (args.length < 1 || !(args[0] instanceof PatientVisitSampleDto request) || request.getVisitId() == null) {
                return;
            }
            Optional<User> userOpt = getAuthenticatedUser();
            if (userOpt.isEmpty() || !notificationService.requiresAdminApproval(userOpt.get())) {
                return;
            }
            // the endpoint has no labId parameter; the service resolves the lab from the visit
            notificationService.raiseVisitSampleChangedAlert(userOpt.get(), request.getVisitId(), action,
                    request.getSampleNames());
        } catch (Exception e) {
            log.warn("Could not raise the visit-sample-changed alert: {}", e.getMessage(), e);
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
