package tiameds.com.tiameds.notification.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.dto.lab.VisitDTO;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.notification.service.NotificationService;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Tells the lab's admins when a non-admin user (DESKROLE, TECHNICIAN) saves a visit with a discount, WITHOUT changing
 * PatientController or VisitController. The endpoints run exactly as before and, once they have succeeded, an
 * information-only DISCOUNT_APPLIED notification (patient, visit, bill and discount details, no approve / reject) is
 * stored for the admins:
 *  - POST /lab/{labId}/add-patient              (new patient, or a new visit for an existing patient)
 *  - POST /lab/{labId}/add-visit/{patientId}
 *  - PUT  /lab/{labId}/update-visit/{visitId}   (only when the discount actually changed)
 *
 * The discount is read back from the saved bill, not trusted from the request. Nothing is raised when no discount was
 * applied, or when an ADMIN / SUPERADMIN did it. A problem while raising the alert is only logged and can never affect
 * the endpoint. Switch off with notification.discount-applied-alert.enabled=false.
 *
 * Runs outside the services' @Transactional, so the bill is already committed when the alert is stored.
 * (Non-admin patient edits are held for admin approval by PatientEditApprovalAspect, so they need no alert here.)
 */
@Slf4j
@Aspect
@Component
// +10: an @AfterReturning with "returning" needs ExposeInvocationInterceptor (HIGHEST_PRECEDENCE) to run first;
// still far outside the services' @Transactional.
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(prefix = "notification.discount-applied-alert", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class DiscountAppliedAlertAspect {

    private final NotificationService notificationService;
    private final UserService userService;

    public DiscountAppliedAlertAspect(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    // Arguments: (Long labId, PatientDTO body)
    @AfterReturning(
            pointcut = "execution(public * tiameds.com.tiameds.controller.lab.PatientController.addPatient(..))",
            returning = "response")
    public void alertOnAddPatient(JoinPoint jp, Object response) {
        try {
            Object[] args = jp.getArgs();
            if (!isSuccess(response) || args.length < 2 || !(args[0] instanceof Long labId)
                    || !(args[1] instanceof PatientDTO body) || !requestHasDiscount(body.getVisit())) {
                return;
            }
            Optional<User> user = nonAdminUser();
            if (user.isEmpty()) {
                return;
            }
            // the response carries the saved patient and its latest (= the new) visit
            Long patientId = null;
            Long visitId = null;
            if (response instanceof ResponseEntity<?> entity && entity.getBody() instanceof Map<?, ?> map
                    && map.get("data") instanceof PatientDTO saved) {
                patientId = saved.getId();
                visitId = saved.getVisit() != null ? saved.getVisit().getVisitId() : null;
            }
            notificationService.raiseDiscountAppliedAlert(user.get(), labId, visitId, patientId, null);
        } catch (Exception e) {
            log.warn("Could not raise the discount-applied alert (add-patient): {}", e.getMessage(), e);
        }
    }

    // Arguments: (Long labId, Long patientId, VisitDTO body). The response has no visit, so the patient's latest is used.
    @AfterReturning(
            pointcut = "execution(public * tiameds.com.tiameds.controller.lab.VisitController.addVisit(..))",
            returning = "response")
    public void alertOnAddVisit(JoinPoint jp, Object response) {
        try {
            Object[] args = jp.getArgs();
            if (!isSuccess(response) || args.length < 3 || !(args[0] instanceof Long labId)
                    || !(args[1] instanceof Long patientId) || !(args[2] instanceof VisitDTO body)
                    || !requestHasDiscount(body)) {
                return;
            }
            Optional<User> user = nonAdminUser();
            if (user.isEmpty()) {
                return;
            }
            notificationService.raiseDiscountAppliedAlert(user.get(), labId, null, patientId, null);
        } catch (Exception e) {
            log.warn("Could not raise the discount-applied alert (add-visit): {}", e.getMessage(), e);
        }
    }

    // Arguments: (Long labId, Long visitId, VisitDTO body). The discount before the update is compared with the one after.
    @Around("execution(public * tiameds.com.tiameds.controller.lab.VisitController.updateVisit(..))")
    public Object alertOnUpdateVisit(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        Long labId = null;
        Long visitId = null;
        String before = null;
        User user = null;
        try {
            if (args.length >= 2 && args[0] instanceof Long l && args[1] instanceof Long v) {
                Optional<User> u = nonAdminUser();
                if (u.isPresent()) {
                    labId = l;
                    visitId = v;
                    user = u.get();
                    before = notificationService.discountKeyOfVisit(visitId);
                }
            }
        } catch (Exception e) {
            before = null; // could not take the snapshot: no alert, the update itself must still run
            log.warn("Could not read the discount before update-visit: {}", e.getMessage(), e);
        }

        Object response = pjp.proceed();   // the update; any exception goes to the caller as before

        try {
            if (before != null && user != null && isSuccess(response)) {
                notificationService.raiseDiscountAppliedAlert(user, labId, visitId, null, before);
            }
        } catch (Exception e) {
            log.warn("Could not raise the discount-applied alert (update-visit): {}", e.getMessage(), e);
        }
        return response;
    }

    private boolean isSuccess(Object response) {
        // the controllers return error responses (not exceptions) when saving failed
        return response instanceof ResponseEntity<?> entity && entity.getStatusCode().is2xxSuccessful();
    }

    /** Cheap pre-check on the request so ordinary registrations cost nothing; the saved bill is what is reported. */
    private boolean requestHasDiscount(VisitDTO visit) {
        if (visit == null) {
            return false;
        }
        if (visit.getBilling() != null && positive(visit.getBilling().getDiscount())) {
            return true;
        }
        return visit.getListOfEachTestDiscount() != null && visit.getListOfEachTestDiscount().stream()
                .anyMatch(d -> d != null && (positive(d.getDiscountAmount()) || positive(d.getDiscountPercent())));
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    /** The logged-in user, only when they are not an ADMIN / SUPERADMIN. */
    private Optional<User> nonAdminUser() {
        Optional<User> user = getAuthenticatedUser();
        return user.isPresent() && notificationService.requiresAdminApproval(user.get()) ? user : Optional.empty();
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
