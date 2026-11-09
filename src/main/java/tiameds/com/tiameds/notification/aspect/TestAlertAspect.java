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
import tiameds.com.tiameds.dto.lab.TestDTO;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.notification.entity.NotificationType;
import tiameds.com.tiameds.notification.service.NotificationService;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;

import java.util.Optional;

/**
 * Raises an information-only TEST_CHANGED alert when a lab test (price list) is added, updated or deleted, WITHOUT
 * changing TestController. These endpoints are ADMIN / SUPERADMIN only, so an ADMIN's change alerts the super
 * admins; a SUPERADMIN's own change raises no alert.
 *
 * Covered: POST /admin/lab/{labId}/add, PUT /admin/lab/{labId}/update/{testId},
 * DELETE /admin/lab/{labId}/remove/{testId}. Only successful (2xx) responses count. A failure here is only logged.
 * Switch off with notification.test-alert.enabled=false.
 *
 * Runs outside the controller's @Transactional (highest precedence), so an alert problem can never roll back the change.
 */
@Slf4j
@Aspect
@Component
// Not HIGHEST_PRECEDENCE itself: an @AfterReturning with "returning" needs ExposeInvocationInterceptor
// (which owns HIGHEST_PRECEDENCE) to run first. +10 is still far outside the controller's @Transactional.
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(prefix = "notification.test-alert", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class TestAlertAspect {

    private final NotificationService notificationService;
    private final UserService userService;

    public TestAlertAspect(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @AfterReturning(
            pointcut = "execution(public * tiameds.com.tiameds.controller.lab.TestController.addTest(..)) "
                    + "|| execution(public * tiameds.com.tiameds.controller.lab.TestController.updateTest(..)) "
                    + "|| execution(public * tiameds.com.tiameds.controller.lab.TestController.removeTest(..))",
            returning = "response")
    public void alertAdmins(JoinPoint jp, Object response) {
        try {
            // TestController returns error responses (not exceptions) with a non-2xx status when the change failed
            if (!(response instanceof ResponseEntity<?> entity) || !entity.getStatusCode().is2xxSuccessful()) {
                return;
            }
            Optional<User> userOpt = getAuthenticatedUser();
            if (userOpt.isEmpty() || notificationService.isSuperAdmin(userOpt.get())) {
                return;
            }

            Object[] args = jp.getArgs();
            if (args.length < 1 || !(args[0] instanceof Long labId)) {
                return;
            }
            TestDTO dto = null;
            for (Object arg : args) {
                if (arg instanceof TestDTO d) {
                    dto = d;
                }
            }

            String[] text;   // {title, message text}
            String name = dto != null ? dto.getName() : null;
            String code = dto != null ? dto.getTestCode() : null;
            switch (jp.getSignature().getName()) {
                case "addTest" -> text = new String[]{"Test added", "added test"};
                case "updateTest" -> text = new String[]{"Test updated", "updated test"};
                case "removeTest" -> {
                    text = new String[]{"Test deleted", "deleted test"};
                    if (args.length > 1 && args[1] instanceof Long testId) {
                        name = "id " + testId;   // the delete request carries no name
                    }
                }
                default -> {
                    return;
                }
            }
            notificationService.raiseChangeAlert(userOpt.get(), labId, NotificationType.TEST_CHANGED,
                    "TEST", text[0], text[1], name, code);
        } catch (Exception e) {
            log.warn("Could not raise the test alert: {}", e.getMessage(), e);
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
