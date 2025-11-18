package tiameds.com.tiameds.controller.onboarding;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.onboarding.VerificationResponseDTO;
import tiameds.com.tiameds.entity.VerificationToken;
import tiameds.com.tiameds.services.onboarding.VerificationTokenService;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for email verification token validation.
 * Validates tokens from email links and redirects to onboarding form.
 */
@Slf4j
@RestController
@RequestMapping("/public/onboarding")
@Tag(name = "Onboarding - Email Verification", description = "Endpoints for validating verification tokens from email links")
public class EmailVerificationController {

    private final VerificationTokenService tokenService;

    @Value("${onboarding.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${onboarding.frontend.onboarding-url:http://localhost:3000/onboarding}")
    private String frontendOnboardingUrl;

    public EmailVerificationController(VerificationTokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * Validates a verification token from email link.
     * If valid, marks token as used and redirects to onboarding form.
     * If invalid, returns error response.
     * 
     * This endpoint is typically accessed via GET from email link.
     * 
     * @param token The verification token from the email link
     * @return Redirect to onboarding form if valid, error response if invalid
     */
    @Operation(summary = "Verify email token", 
               description = "Validates verification token from email link. If valid, redirects to onboarding form. Token is single-use only.")
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmailToken(@RequestParam("token") String token) {
        log.info("Email verification token validation requested");

        if (token == null || token.trim().isEmpty()) {
            log.warn("Empty token provided for verification");
            return buildErrorResponse("Invalid verification token. Please request a new verification email.");
        }

        // Validate token (this checks but doesn't consume - we'll consume in onboarding submission)
        Optional<VerificationToken> tokenOpt = tokenService.validateToken(token);

        if (tokenOpt.isEmpty()) {
            log.warn("Invalid or expired token provided for verification");
            return buildErrorResponse("Invalid or expired verification token. Please request a new verification email.");
        }

        VerificationToken verificationToken = tokenOpt.get();
        String email = verificationToken.getEmail();

        log.info("Token validated successfully for email: {}", email);

        // Build onboarding URL with token (token will be consumed when form is submitted)
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String onboardingUrl = frontendOnboardingUrl + "?token=" + encodedToken;

        // Return redirect response
        VerificationResponseDTO response = VerificationResponseDTO.builder()
                .valid(true)
                .email(email)
                .message("Email verified successfully. Please complete the onboarding form.")
                .redirectUrl(onboardingUrl)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * API endpoint for token validation (returns JSON instead of redirect).
     * Useful for frontend that wants to handle redirect programmatically.
     */
    @Operation(summary = "Validate email token (API)", 
               description = "Validates verification token and returns JSON response with redirect URL. Token is NOT consumed here.")
    @PostMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateTokenApi(@RequestParam("token") String token) {
        log.info("Token validation API called");

        if (token == null || token.trim().isEmpty()) {
            return buildErrorResponse("Token is required");
        }

        Optional<VerificationToken> tokenOpt = tokenService.validateToken(token);

        if (tokenOpt.isEmpty()) {
            return buildErrorResponse("Invalid or expired verification token");
        }

        VerificationToken verificationToken = tokenOpt.get();
        String email = verificationToken.getEmail();
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String onboardingUrl = frontendOnboardingUrl + "?token=" + encodedToken;

        VerificationResponseDTO response = VerificationResponseDTO.builder()
                .valid(true)
                .email(email)
                .message("Token is valid")
                .redirectUrl(onboardingUrl)
                .build();

        return ApiResponseHelper.successResponseWithDataAndMessage("Token is valid", HttpStatus.OK, response);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message) {
        return ApiResponseHelper.errorResponseWithMessage(message, HttpStatus.BAD_REQUEST);
    }
}

