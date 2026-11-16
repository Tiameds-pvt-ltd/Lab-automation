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
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.notification.service.NotificationService;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;

import java.util.Optional;

/**
 * Tells the super admins when a report is updated, WITHOUT changing ReportGeneration:
 * PUT /lab/{labId}/report/by-id/{reportId} runs exactly as before and, once it has succeeded, an information-only
 * REPORT_UPDATED notification (no approve / reject) is stored for the super admins.
 *
 * Updates made by a SUPERADMIN raise no alert. A problem while raising the alert is only logged and can never affect
 * the update. Switch off with notification.report-update-alert.enabled=false.
 *
 * Runs outside the controller's @Transactional, so the update is already committed when the alert is stored.
 */
@Slf4j
@Aspect
@Component
// +10: an @AfterReturning with "returning" needs ExposeInvocationInterceptor (HIGHEST_PRECEDENCE) to run first;
// still far outside the controller's @Transactional.
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(prefix = "notification.report-update-alert", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ReportUpdateAlertAspect {

    private final NotificationService notificationService;
    private final UserService userService;

    public ReportUpdateAlertAspect(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    // Arguments: (Long labId, Long reportId, List<ReportDto> body, HttpServletRequest request)
    @AfterReturning(
            pointcut = "execution(public * tiameds.com.tiameds.controller.sampleassociation.ReportGeneration.updateReportByReportId(..))",
            returning = "response")
    public void alertSuperAdmins(JoinPoint jp, Object response) {
        try {
            // the controller returns error responses (not exceptions) when the update failed
            if (!(response instanceof ResponseEntity<?> entity) || !entity.getStatusCode().is2xxSuccessful()) {
                return;
            }
            Object[] args = jp.getArgs();
            if (args.length < 2 || !(args[0] instanceof Long labId) || !(args[1] instanceof Long reportId)) {
                return;
            }
            Optional<User> userOpt = getAuthenticatedUser();
            if (userOpt.isEmpty() || notificationService.isSuperAdmin(userOpt.get())) {
                return;
            }
            notificationService.raiseReportUpdatedAlert(userOpt.get(), labId, reportId);
        } catch (Exception e) {
            log.warn("Could not raise the report-updated alert: {}", e.getMessage(), e);
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
