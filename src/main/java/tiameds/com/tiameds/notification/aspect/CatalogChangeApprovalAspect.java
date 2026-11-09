package tiameds.com.tiameds.notification.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import tiameds.com.tiameds.dto.lab.TestDTO;
import tiameds.com.tiameds.dto.lab.TestReferenceDTO;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.notification.service.NotificationException;
import tiameds.com.tiameds.notification.service.NotificationService;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.Optional;

/**
 * Holds test and test reference changes for approval, WITHOUT changing TestController / TestReferenceController:
 * the endpoint is intercepted and, instead of changing anything, the request is stored as a PENDING notification.
 *
 *  - Tests (ADMIN only endpoints): add / update / remove  -> a SUPERADMIN approves or rejects
 *  - Test references: add / update (both forms) / delete / delete-all
 *        TECHNICIAN / DESKROLE -> the lab's admins approve, ADMIN -> a SUPERADMIN approves
 *
 * SUPERADMIN calls are not intercepted and apply immediately, as before. CSV upload is not held (the file would
 * have to be stored); it only raises an alert, see TestReferenceAlertAspect.
 * Switch off with notification.catalog-change-approval.enabled=false (config only, no code change).
 *
 * HIGHEST_PRECEDENCE like PatientEditApprovalAspect: an intercepted call never reaches the controller. This is an
 * @Around advice, so (unlike @AfterReturning with a returning binding) that precedence is fine.
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "notification.catalog-change-approval", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class CatalogChangeApprovalAspect {

    private static final String MSG = "Your change was sent for approval. It will be applied once it is approved.";

    private final NotificationService notificationService;
    private final UserService userService;

    public CatalogChangeApprovalAspect(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    // ---- tests: (Long labId, TestDTO body, HttpServletRequest) / (labId, testId, TestDTO, request) / (labId, testId, request)

    @Around("execution(public * tiameds.com.tiameds.controller.lab.TestController.addTest(..))")
    public Object holdTestAdd(ProceedingJoinPoint pjp) throws Throwable {
        Object[] a = pjp.getArgs();
        Optional<User> user = holdableUser();
        if (user.isEmpty() || a.length < 2 || !(a[0] instanceof Long labId) || !(a[1] instanceof TestDTO body)) {
            return pjp.proceed();
        }
        return submit(() -> notificationService.submitTestChange(
                user.get(), labId, NotificationService.OP_ADD, null, body));
    }

    @Around("execution(public * tiameds.com.tiameds.controller.lab.TestController.updateTest(..))")
    public Object holdTestUpdate(ProceedingJoinPoint pjp) throws Throwable {
        Object[] a = pjp.getArgs();
        Optional<User> user = holdableUser();
        if (user.isEmpty() || a.length < 3 || !(a[0] instanceof Long labId) || !(a[1] instanceof Long testId)
                || !(a[2] instanceof TestDTO body)) {
            return pjp.proceed();
        }
        return submit(() -> notificationService.submitTestChange(
                user.get(), labId, NotificationService.OP_UPDATE, testId, body));
    }

    @Around("execution(public * tiameds.com.tiameds.controller.lab.TestController.removeTest(..))")
    public Object holdTestRemove(ProceedingJoinPoint pjp) throws Throwable {
        Object[] a = pjp.getArgs();
        Optional<User> user = holdableUser();
        if (user.isEmpty() || a.length < 2 || !(a[0] instanceof Long labId) || !(a[1] instanceof Long testId)) {
            return pjp.proceed();
        }
        return submit(() -> notificationService.submitTestChange(
                user.get(), labId, NotificationService.OP_DELETE, testId, null));
    }

    // ---- test references

    @Around("execution(public * tiameds.com.tiameds.controller.lab.TestReferenceController.addTestReference(..))")
    public Object holdReferenceAdd(ProceedingJoinPoint pjp) throws Throwable {
        Object[] a = pjp.getArgs();
        Optional<User> user = holdableUser();
        if (user.isEmpty() || a.length < 1 || !(a[0] instanceof TestReferenceDTO body) || body.getLabId() == null) {
            return pjp.proceed();   // the controller reports the missing lab id itself
        }
        return submit(() -> notificationService.submitTestReferenceChange(
                user.get(), body.getLabId(), NotificationService.OP_ADD, null, body));
    }

    // PUT /{labId}/{testReferenceId}  (Long labId, Long testReferenceId, TestReferenceDTO body)
    @Around("execution(public * tiameds.com.tiameds.controller.lab.TestReferenceController.updateTestReference(..))")
    public Object holdReferenceUpdateByPath(ProceedingJoinPoint pjp) throws Throwable {
        Object[] a = pjp.getArgs();
        Optional<User> user = holdableUser();
        if (user.isEmpty() || a.length < 3 || !(a[0] instanceof Long labId) || !(a[1] instanceof Long refId)
                || !(a[2] instanceof TestReferenceDTO body)) {
            return pjp.proceed();
        }
        return submit(() -> notificationService.submitTestReferenceChange(
                user.get(), labId, NotificationService.OP_UPDATE, refId, body));
    }

    // PUT /update  (TestReferenceDTO body with labId and testReferenceCode)
    @Around("execution(public * tiameds.com.tiameds.controller.lab.TestReferenceController.updateTestReferenceByCode(..))")
    public Object holdReferenceUpdateByCode(ProceedingJoinPoint pjp) throws Throwable {
        Object[] a = pjp.getArgs();
        Optional<User> user = holdableUser();
        if (user.isEmpty() || a.length < 1 || !(a[0] instanceof TestReferenceDTO body) || body.getLabId() == null) {
            return pjp.proceed();
        }
        return submit(() -> notificationService.submitTestReferenceChange(
                user.get(), body.getLabId(), NotificationService.OP_UPDATE, body.getId(), body));
    }

    // DELETE /delete  (TestReferenceDTO body with labId and id or testReferenceCode)
    @Around("execution(public * tiameds.com.tiameds.controller.lab.TestReferenceController.deleteTestReference(..))")
    public Object holdReferenceDelete(ProceedingJoinPoint pjp) throws Throwable {
        Object[] a = pjp.getArgs();
        Optional<User> user = holdableUser();
        if (user.isEmpty() || a.length < 1 || !(a[0] instanceof TestReferenceDTO body) || body.getLabId() == null) {
            return pjp.proceed();
        }
        return submit(() -> notificationService.submitTestReferenceChange(
                user.get(), body.getLabId(), NotificationService.OP_DELETE, body.getId(), body));
    }

    // DELETE /{labId}/delete-all
    @Around("execution(public * tiameds.com.tiameds.controller.lab.TestReferenceController.deleteAllTestReferences(..))")
    public Object holdReferenceDeleteAll(ProceedingJoinPoint pjp) throws Throwable {
        Object[] a = pjp.getArgs();
        Optional<User> user = holdableUser();
        if (user.isEmpty() || a.length < 1 || !(a[0] instanceof Long labId)) {
            return pjp.proceed();
        }
        return submit(() -> notificationService.submitTestReferenceChange(
                user.get(), labId, NotificationService.OP_DELETE_ALL, null, null));
    }

    // ----

    private Object submit(java.util.function.Supplier<Object> action) {
        try {
            Object result = action.get();
            return ApiResponseHelper.successResponseWithDataAndMessage(MSG, HttpStatus.ACCEPTED, result);
        } catch (NotificationException e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), e.getStatus());
        }
    }

    /** The caller, unless they are a SUPERADMIN (who is never held) or not logged in. */
    private Optional<User> holdableUser() {
        return getAuthenticatedUser().filter(u -> !notificationService.isSuperAdmin(u));
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
