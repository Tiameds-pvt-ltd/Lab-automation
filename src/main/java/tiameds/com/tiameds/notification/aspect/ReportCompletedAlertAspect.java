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
import tiameds.com.tiameds.dto.lab.ReportDto;
import tiameds.com.tiameds.dto.lab.ReportRequestDto;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.notification.service.NotificationService;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;

import java.util.Optional;

/**
 * Tells the lab's DESKROLE users that a report is completed (please take the print), WITHOUT changing ReportGeneration:
 * POST /lab/{labId}/report runs exactly as before and, once the results were added successfully, an information-only
 * REPORT_COMPLETED notification (no approve / reject) is stored for the desk.
 *
 * A problem while raising the alert is only logged and can never affect saving the results.
 * Switch off with notification.report-completed-alert.enabled=false.
 *
 * Runs outside the controller's @Transactional, so the results are already committed when the alert is stored.
 */
@Slf4j
@Aspect
@Component
// +10: an @AfterReturning with "returning" needs ExposeInvocationInterceptor (HIGHEST_PRECEDENCE) to run first;
// still far outside the controller's @Transactional.
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(prefix = "notification.report-completed-alert", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ReportCompletedAlertAspect {

    private final NotificationService notificationService;
    private final UserService userService;

    public ReportCompletedAlertAspect(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    // Arguments: (Long labId, ReportRequestDto body, HttpServletRequest request)
    @AfterReturning(
            pointcut = "execution(public * tiameds.com.tiameds.controller.sampleassociation.ReportGeneration.createReport(..))",
            returning = "response")
    public void alertDesk(JoinPoint jp, Object response) {
        try {
            // the controller returns error responses (not exceptions) when adding the results failed
            if (!(response instanceof ResponseEntity<?> entity) || !entity.getStatusCode().is2xxSuccessful()) {
                return;
            }
            Object[] args = jp.getArgs();
            if (args.length < 2 || !(args[0] instanceof Long labId) || !(args[1] instanceof ReportRequestDto body)) {
                return;
            }
            Long visitId = null;
            if (body.getTestData() != null) {
                visitId = body.getTestData().stream()
                        .map(ReportDto::getVisitId)
                        .filter(id -> id != null)
                        .findFirst()
                        .orElse(null);
            }
            Optional<User> userOpt = getAuthenticatedUser();
            if (userOpt.isEmpty()) {
                return;
            }
            notificationService.raiseReportCompletedAlert(userOpt.get(), labId, visitId);
        } catch (Exception e) {
            log.warn("Could not raise the report-completed alert: {}", e.getMessage(), e);
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
