package tiameds.com.tiameds.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.notification.dto.ApproveNotificationRequest;
import tiameds.com.tiameds.notification.dto.RejectNotificationRequest;
import tiameds.com.tiameds.notification.entity.NotificationStatus;
import tiameds.com.tiameds.notification.service.NotificationException;
import tiameds.com.tiameds.notification.service.NotificationService;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * Notifications and approval requests. Base path /api/v1/notifications.
 * Not listed in SpringSecurityConfig on purpose (falls under "any other request: authenticated");
 * the ADMIN-only rules are enforced in NotificationService, and every call also requires lab membership.
 */
@Slf4j
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Alerts and approval requests for lab admins")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @Operation(summary = "Request a patient edit (needs admin approval)",
            description = "Same body as PUT /lab/{labId}/update-patient-details/{patientId}, but the patient is NOT changed. "
                    + "The proposed values are stored as a PENDING notification for the admins of the lab.")
    @PostMapping("/lab/{labId}/patient-edit-requests/{patientId}")
    public ResponseEntity<?> submitPatientEditRequest(@PathVariable Long labId,
                                                      @PathVariable Long patientId,
                                                      @RequestBody PatientDTO patientDTO) {
        return handle("Edit request sent for admin approval", HttpStatus.CREATED,
                user -> notificationService.submitPatientEditRequest(user, labId, patientId, patientDTO));
    }

    @Operation(summary = "List notifications of the lab (admin)",
            description = "Newest first. Optional status filter: PENDING, APPROVED or REJECTED.")
    @GetMapping("/lab/{labId}")
    public ResponseEntity<?> getLabNotifications(@PathVariable Long labId,
                                                 @RequestParam(value = "status", required = false) String status) {
        NotificationStatus parsed = null;
        if (status != null && !status.isBlank()) {
            try {
                parsed = NotificationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return ApiResponseHelper.errorResponseWithMessage(
                        "Invalid status. Use PENDING, APPROVED or REJECTED", HttpStatus.BAD_REQUEST);
            }
        }
        final NotificationStatus filter = parsed;
        return handle("Notifications fetched successfully", HttpStatus.OK,
                user -> notificationService.getLabNotifications(user, labId, filter));
    }

    @Operation(summary = "Number of pending notifications (admin)", description = "For the bell icon badge.")
    @GetMapping("/lab/{labId}/pending-count")
    public ResponseEntity<?> getPendingCount(@PathVariable Long labId) {
        return handle("Pending count fetched successfully", HttpStatus.OK,
                user -> notificationService.getPendingCount(user, labId));
    }

    @Operation(summary = "My requests",
            description = "The requests the logged-in user raised in this lab, with their status and rejection reason.")
    @GetMapping("/lab/{labId}/my-requests")
    public ResponseEntity<?> getMyRequests(@PathVariable Long labId) {
        return handle("Your requests fetched successfully", HttpStatus.OK,
                user -> notificationService.getMyRequests(user, labId));
    }

    @Operation(summary = "Notification detail",
            description = "Shows the current patient values next to the proposed ones. Admins, or the user who raised it.")
    @GetMapping("/lab/{labId}/{notificationId}")
    public ResponseEntity<?> getNotification(@PathVariable Long labId, @PathVariable Long notificationId) {
        return handle("Notification fetched successfully", HttpStatus.OK,
                user -> notificationService.getNotification(user, labId, notificationId));
    }

    @Operation(summary = "Approve a request (admin)",
            description = "Applies the proposed patient details using the existing update logic and marks the request APPROVED.")
    @PutMapping("/lab/{labId}/{notificationId}/approve")
    public ResponseEntity<?> approve(@PathVariable Long labId,
                                     @PathVariable Long notificationId,
                                     @RequestBody(required = false) ApproveNotificationRequest body) {
        String reason = body != null ? body.getReason() : null;
        return handle("Request approved and patient updated", HttpStatus.OK,
                user -> notificationService.approve(user, labId, notificationId, reason));
    }

    @Operation(summary = "Reject a request (admin)",
            description = "The patient is left unchanged. A reason is required and is shown to the requester.")
    @PutMapping("/lab/{labId}/{notificationId}/reject")
    public ResponseEntity<?> reject(@PathVariable Long labId,
                                    @PathVariable Long notificationId,
                                    @RequestBody(required = false) RejectNotificationRequest body) {
        String reason = body != null ? body.getReason() : null;
        return handle("Request rejected", HttpStatus.OK,
                user -> notificationService.reject(user, labId, notificationId, reason));
    }

    private ResponseEntity<?> handle(String successMessage, HttpStatus successStatus, Function<User, Object> action) {
        Optional<User> currentUser = getAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponseWithMessage("User not found", HttpStatus.UNAUTHORIZED);
        }
        try {
            Object result = action.apply(currentUser.get());
            return ApiResponseHelper.successResponseWithDataAndMessage(successMessage, successStatus, result);
        } catch (NotificationException e) {
            return ApiResponseHelper.errorResponseWithMessage(e.getMessage(), e.getStatus());
        } catch (Exception e) {
            log.error("Notification request failed: {}", e.getMessage(), e);
            return ApiResponseHelper.errorResponseWithMessage(e.getMessage(), HttpStatus.BAD_REQUEST);
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
