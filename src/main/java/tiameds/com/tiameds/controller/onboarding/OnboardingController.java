package tiameds.com.tiameds.controller.onboarding;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tiameds.com.tiameds.dto.onboarding.OnboardingRequestDTO;
import tiameds.com.tiameds.dto.onboarding.OnboardingResponseDTO;
import tiameds.com.tiameds.services.lab.LabDefaultDataService;
import tiameds.com.tiameds.services.onboarding.OnboardingService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for onboarding form submission.
 * Handles user account creation and Lab (organization) creation after email verification.
 */
@Slf4j
@RestController
@RequestMapping("/public/onboarding")
@Tag(name = "Onboarding - Form Submission", description = "Endpoints for submitting onboarding form and creating user account with Lab")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final LabDefaultDataService labDefaultDataService;

    public OnboardingController(OnboardingService onboardingService,
                                LabDefaultDataService labDefaultDataService) {
        this.onboardingService = onboardingService;
        this.labDefaultDataService = labDefaultDataService;
    }

    /**
     * Completes the onboarding process.
     * 
     * Flow:
     * 1. Validates and consumes the verification token (single-use enforcement)
     * 2. Creates user account with provided information
     * 3. Creates Lab (organization) with provided information
     * 4. Associates user with the lab
     * 5. Activates the account
     * 
     * All operations are wrapped in a transaction for atomicity.
     * 
     * @param request Onboarding request with user and lab information
     * @return Onboarding response with created user and lab details
     */
    @Operation(summary = "Complete onboarding", 
               description = "Submits onboarding form, validates token, creates user account and Lab. Token is consumed (single-use only).")
    @PostMapping("/complete")
    public ResponseEntity<Map<String, Object>> completeOnboarding(
            @Valid @RequestBody OnboardingRequestDTO request) {
        
        log.info("Onboarding completion requested for email: {}", request.getEmail());

        try {
            // Validate token and create user + lab in a single transaction
            OnboardingResponseDTO response = onboardingService.completeOnboarding(request);
            
            // Safe logging with null checks
            String username = response != null ? response.getUsername() : "unknown";
            String labName = response != null ? response.getLabName() : "unknown";
            log.info("Onboarding completed successfully for user: {} (Lab: {})", username, labName);

            // Safe message extraction with fallback
            String message = (response != null && response.getMessage() != null) 
                    ? response.getMessage() 
                    : "Onboarding completed successfully";

            // Return response immediately - don't wait for default data upload
            ResponseEntity<Map<String, Object>> successResponse = ApiResponseHelper.successResponseWithDataAndMessage(
                    message,
                    HttpStatus.CREATED,
                    response);
            
            // Trigger default data upload asynchronously - never block the response
            triggerDefaultDataUploadAsync(response);
            
            return successResponse;

        } catch (IllegalArgumentException e) {
            log.warn("Onboarding failed due to validation error: {}", e.getMessage());
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST,
                    null);
        } catch (Exception e) {
            log.error("Unexpected error during onboarding for email {}: {}", 
                    request.getEmail(), e.getMessage(), e);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "An error occurred during onboarding. Please try again or contact support.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    null);
        }
    }

    /**
     * Triggers default data upload asynchronously without blocking the HTTP response.
     * This ensures onboarding response is returned immediately even if upload takes time.
     */
    private void triggerDefaultDataUploadAsync(OnboardingResponseDTO response) {
        if (response == null) {
            return;
        }
        
        Long labId = response.getLabId();
        Long userId = response.getUserId();
        
        if (labId == null || userId == null) {
            log.warn("Skipping default data upload due to null labId or userId (labId={}, userId={})", labId, userId);
            return;
        }
        
        // Execute asynchronously - don't block the HTTP response
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting async default data upload for lab {} (userId: {})", labId, userId);
                labDefaultDataService.uploadDefaultData(labId, userId);
                log.info("Completed async default data upload for lab {}", labId);
            } catch (Exception e) {
                // Log but never throw - onboarding succeeded, this is just a convenience feature
                log.error("Default data upload failed but onboarding succeeded for lab {}: {}", 
                        labId, e.getMessage(), e);
            }
        });
    }
}

