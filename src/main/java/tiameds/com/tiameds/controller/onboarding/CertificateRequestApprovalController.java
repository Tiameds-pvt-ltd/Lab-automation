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
import tiameds.com.tiameds.services.lab.LabCertificateRequestService;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * Review of lab certificate requests, called by the independent platform-admin module.
 * Like the onboarding review, these endpoints have no authentication (permitAll for /platform-admin/** in
 * SpringSecurityConfig), so they must only be reachable from the platform-admin side, e.g. blocked for
 * /platform-admin/** at the load balancer / WAF.
 */
@Slf4j
@RestController
@RequestMapping("/platform-admin/certificate-requests")
@Tag(name = "Certificate Requests - Approval", description = "Review (approve / reject) lab certificate requests")
public class CertificateRequestApprovalController {

    private final LabCertificateRequestService certificateRequestService;

    public CertificateRequestApprovalController(LabCertificateRequestService certificateRequestService) {
        this.certificateRequestService = certificateRequestService;
    }

    @Operation(summary = "List certificate requests",
            description = "Lists certificate requests by status (PENDING by default, oldest first).")
    @GetMapping
    public ResponseEntity<?> getRequests(@RequestParam(defaultValue = "PENDING") String status) {
        LabApprovalStatus parsed;
        try {
            parsed = LabApprovalStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Invalid status. Allowed: PENDING, APPROVED, REJECTED", HttpStatus.BAD_REQUEST, null);
        }
        return handle("Certificate requests fetched successfully", () -> certificateRequestService.getRequests(parsed));
    }

    @Operation(summary = "Approve certificate request",
            description = "Makes the new file the lab's certificate. The replaced file is kept. Only a PENDING request can be approved.")
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<?> approve(@PathVariable Long requestId) {
        return handle("Certificate request approved", () -> certificateRequestService.approve(requestId));
    }

    @Operation(summary = "Reject certificate request",
            description = "Leaves the lab's certificate unchanged and records the reason. Only a PENDING request can be rejected.")
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<?> reject(@PathVariable Long requestId, @Valid @RequestBody OnboardingRejectRequestDTO request) {
        return handle("Certificate request rejected", () -> certificateRequestService.reject(requestId, request.getReason()));
    }

    private ResponseEntity<?> handle(String successMessage, Supplier<Object> action) {
        try {
            return ApiResponseHelper.successResponseWithDataAndMessage(successMessage, HttpStatus.OK, action.get());
        } catch (NoSuchElementException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.NOT_FOUND, null);
        } catch (IllegalStateException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.CONFLICT, null);
        } catch (Exception e) {
            log.error("Certificate request review failed: {}", e.getMessage(), e);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Operation failed. No changes were saved.", HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }
}
