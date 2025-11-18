package tiameds.com.tiameds.controller.onboarding;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.onboarding.EmailRequestDTO;
import tiameds.com.tiameds.services.email.EmailService;
import tiameds.com.tiameds.services.onboarding.EmailRateLimitService;
import tiameds.com.tiameds.services.onboarding.VerificationTokenService;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.Map;

/**
 * Controller for initiating the onboarding flow.
 * Handles email submission and verification link sending.
 */
@Slf4j
@RestController
@RequestMapping("/public/onboarding")
@Tag(name = "Onboarding - Email Verification", description = "Endpoints for requesting verification emails during onboarding")
public class AuthOnboardingController {

    private final VerificationTokenService tokenService;
    private final EmailService emailService;
    private final EmailRateLimitService rateLimitService;

    @Value("${onboarding.frontend.verification-url:http://localhost:3000/verify-email}")
    private String frontendVerificationUrl;

    public AuthOnboardingController(
            VerificationTokenService tokenService,
            EmailService emailService,
            EmailRateLimitService rateLimitService) {
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Request a verification email for onboarding.
     * 
     * Flow:
     * 1. User submits their email
     * 2. System checks rate limit (max 3 emails per hour)
     * 3. If allowed, generates secure token and stores hash in DB
     * 4. Sends verification link via email
     * 
     * @param request Email request DTO
     * @return Success or rate limit error
     */
    @Operation(summary = "Request verification email", 
               description = "Submits email address and receives a verification link. Rate limited to 3 emails per hour per email address.")
    @PostMapping("/request-verification")
    public ResponseEntity<Map<String, Object>> requestVerificationEmail(
            @Valid @RequestBody EmailRequestDTO request) {
        
        String email = request.getEmail().trim().toLowerCase();
        log.info("Verification email requested for: {}", email);

        // Check rate limit (database-based, no in-memory cache)
        if (!rateLimitService.canSendEmail(email)) {
            long remaining = rateLimitService.getRemainingEmails(email);
            int windowMinutes = rateLimitService.getWindowMinutes();
            
            log.warn("Rate limit exceeded for email: {}", email);
            return ApiResponseHelper.errorResponseWithMessage(
                    String.format("Too many verification emails sent. Please try again after %d minutes. Remaining attempts: %d", 
                            windowMinutes, remaining),
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        try {
            // Generate secure token and store hash in database
            String plainToken = tokenService.generateAndStoreToken(email);

            // Send verification email with link
            emailService.sendVerificationEmail(email, plainToken, frontendVerificationUrl);

            log.info("Verification email sent successfully to: {}", email);
            
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Verification email sent successfully. Please check your inbox.",
                    HttpStatus.OK,
                    null);

        } catch (EmailService.EmailException e) {
            log.error("Failed to send verification email to {}: {}", email, e.getMessage(), e);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Failed to send verification email. Please try again later.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    null);
        } catch (Exception e) {
            log.error("Unexpected error processing verification request for {}: {}", email, e.getMessage(), e);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "An error occurred. Please try again later.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    null);
        }
    }

    /**
     * Resend verification email (same logic as request, but with explicit resend endpoint)
     */
    @Operation(summary = "Resend verification email", 
               description = "Resends a verification email. Subject to same rate limiting as request-verification.")
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerificationEmail(
            @Valid @RequestBody EmailRequestDTO request) {
        // Same logic as request-verification
        return requestVerificationEmail(request);
    }
}

