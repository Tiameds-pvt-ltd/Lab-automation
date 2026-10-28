package tiameds.com.tiameds.controller.onboarding;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.onboarding.OnboardingRejectRequestDTO;
import tiameds.com.tiameds.entity.LabApprovalStatus;
import tiameds.com.tiameds.services.onboarding.OnboardingApprovalService;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * Review of onboarding requests, called by the independent platform-admin module.
 * These endpoints have no authentication (permitAll in SpringSecurityConfig), so they must only be
 * reachable from the platform-admin side, e.g. blocked for /platform-admin/** at the load balancer / WAF.
 */
@Slf4j
@RestController
@RequestMapping("/platform-admin/onboarding-requests")
@Tag(name = "Onboarding - Approval", description = "Review (approve / reject) lab onboarding requests")
public class OnboardingApprovalController {

    private final OnboardingApprovalService approvalService;

    public OnboardingApprovalController(OnboardingApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Operation(summary = "List onboarding requests",
            description = "Lists onboarding requests by status (PENDING by default, oldest first).")
    @GetMapping
    public ResponseEntity<?> getRequests(@RequestParam(defaultValue = "PENDING") String status) {
        LabApprovalStatus parsed;
        try {
            parsed = LabApprovalStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Invalid status. Allowed: PENDING, APPROVED, REJECTED", HttpStatus.BAD_REQUEST, null);
        }
        return handle("Onboarding requests fetched successfully", () -> approvalService.getRequests(parsed));
    }

    @Operation(summary = "Approve onboarding request",
            description = "Activates the lab. Only a PENDING request can be approved.")
    @PostMapping("/{labId}/approve")
    public ResponseEntity<?> approve(@PathVariable Long labId) {
        return handle("Onboarding request approved", () -> approvalService.approve(labId));
    }

    @Operation(summary = "Reject onboarding request",
            description = "Keeps the lab inactive and records the reason. Only a PENDING request can be rejected.")
    @PostMapping("/{labId}/reject")
    public ResponseEntity<?> reject(@PathVariable Long labId, @Valid @RequestBody OnboardingRejectRequestDTO request) {
        return handle("Onboarding request rejected", () -> approvalService.reject(labId, request.getReason()));
    }

    private ResponseEntity<?> handle(String successMessage, Supplier<Object> action) {
        try {
            return ApiResponseHelper.successResponseWithDataAndMessage(successMessage, HttpStatus.OK, action.get());
        } catch (NoSuchElementException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.NOT_FOUND, null);
        } catch (IllegalStateException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.CONFLICT, null);
        } catch (Exception e) {
            log.error("Onboarding approval operation failed: {}", e.getMessage(), e);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Operation failed. No changes were saved.", HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }
}
