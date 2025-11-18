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
            autoUploadDefaultTestData(response.getLabId(), response.getUserId());

            log.info("Onboarding completed successfully for user: {} (Lab: {})", 
                    response.getUsername(), response.getLabName());

            return ApiResponseHelper.successResponseWithDataAndMessage(
                    response.getMessage(),
                    HttpStatus.CREATED,
                    response);

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

    private void autoUploadDefaultTestData(Long labId, Long userId) {
        try {
            labDefaultDataService.uploadDefaultData(labId, userId);
        } catch (Exception e) {
            log.error("Default data upload failed for lab {}: {}", labId, e.getMessage(), e);
        }
    }
}

