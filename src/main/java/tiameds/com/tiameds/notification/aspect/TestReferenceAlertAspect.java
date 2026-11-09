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
import tiameds.com.tiameds.dto.lab.TestReferenceDTO;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.notification.entity.NotificationType;
import tiameds.com.tiameds.notification.service.NotificationService;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;

import java.util.Optional;

/**
 * Raises an information-only TEST_REFERENCE_CHANGED alert when test reference data is added, updated or deleted,
 * WITHOUT changing TestReferenceController: the endpoint runs exactly as before and, once it succeeded, the alert
 * is stored. Who gets it: TECHNICIAN / DESKROLE changes -> the lab's admins; ADMIN changes -> super admins only;
 * SUPERADMIN changes -> no alert (see NotificationService.raiseChangeAlert).
 *
 * Only the CSV upload (POST /{labId}/csv/upload) is covered here: add / update / delete are held for approval by
 * CatalogChangeApprovalAspect instead, and the upload can't be held because the file would have to be stored.
 * A failure here is only logged.
 * Switch off with notification.test-reference-alert.enabled=false.
 *
 * Runs outside the controller's @Transactional (highest precedence), so the change is already committed and an
 * alert problem can never roll it back.
 */
@Slf4j
@Aspect
@Component
// Not HIGHEST_PRECEDENCE itself: an @AfterReturning with "returning" needs ExposeInvocationInterceptor
// (which owns HIGHEST_PRECEDENCE) to run first. +10 is still far outside the controller's @Transactional.
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(prefix = "notification.test-reference-alert", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class TestReferenceAlertAspect {

    private final NotificationService notificationService;
    private final UserService userService;

    public TestReferenceAlertAspect(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @AfterReturning(
            pointcut = "execution(public * tiameds.com.tiameds.controller.lab.TestReferenceController.uploadCsv(..))",
            returning = "response")
    public void alertAdmins(JoinPoint jp, Object response) {
        try {
            // the controller returns error responses (not exceptions) when the change failed
            if (!(response instanceof ResponseEntity<?> entity) || !entity.getStatusCode().is2xxSuccessful()) {
                return;
            }
            Optional<User> userOpt = getAuthenticatedUser();
            // SUPERADMIN changes need no alert; the service decides the audience for everyone else
            if (userOpt.isEmpty() || notificationService.isSuperAdmin(userOpt.get())) {
                return;
            }

            Object[] args = jp.getArgs();
            TestReferenceDTO dto = null;
            Long labId = null;
            for (Object arg : args) {
                if (arg instanceof TestReferenceDTO d) {
                    dto = d;
                }
            }
            if (args.length > 0 && args[0] instanceof Long id) {
                labId = id;                      // path based endpoints: (labId, ...)
            } else if (dto != null) {
                labId = dto.getLabId();          // body based endpoints
            }
            if (labId == null) {
                return;
            }

            notificationService.raiseChangeAlert(userOpt.get(), labId, NotificationType.TEST_REFERENCE_CHANGED,
                    "TEST_REFERENCE", "Test references uploaded", "uploaded test references (CSV)", null, null);
        } catch (Exception e) {
            log.warn("Could not raise the test-reference alert: {}", e.getMessage(), e);
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
