package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tiameds.com.tiameds.dto.lab.LabOnboardingDetailsDTO;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.LabOnboardingDetailsService;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

/**
 * Edit of the organization and lab details that were submitted during onboarding, for use after login
 * when something was entered wrongly.
 * Covered by the existing /lab/admin/** rule (ADMIN / SUPERADMIN); the user must also be a member of the lab.
 *
 * Logo and certificate files are not part of this endpoint, use /lab/admin/{labId}/documents for those.
 */
@Slf4j
@RestController
@RequestMapping("/lab/admin/{labId}/onboarding-details")
@Tag(name = "Lab Onboarding Details", description = "View and edit the organization and lab details submitted during onboarding")
public class LabOnboardingDetailsController {

    private final LabOnboardingDetailsService labOnboardingDetailsService;
    private final UserService userService;

    public LabOnboardingDetailsController(LabOnboardingDetailsService labOnboardingDetailsService,
                                          UserService userService) {
        this.labOnboardingDetailsService = labOnboardingDetailsService;
        this.userService = userService;
    }

    /**
     * Returns the current values so the edit form can be pre-filled.
     */
    @Operation(summary = "Get onboarding details",
            description = "Returns the current organization and lab details that were entered during onboarding.")
    @GetMapping
    public ResponseEntity<?> getDetails(@PathVariable Long labId) {
        return handle("Onboarding details fetched successfully",
                user -> labOnboardingDetailsService.getDetails(user, labId));
    }

    /**
     * Edits the onboarding details.
     * Only the fields that are sent are changed (missing / null = unchanged). Mandatory fields cannot be blanked,
     * optional fields can be cleared with "". The edit is all-or-nothing: if any rule fails nothing is saved.
     */
    @Operation(summary = "Edit onboarding details",
            description = "Updates the organization and/or lab details entered during onboarding. Only the fields that are sent "
                    + "are changed. Mandatory fields cannot be blanked; optional fields can be cleared with an empty string. "
                    + "Files are not handled here, use the lab documents endpoint.")
    @PutMapping
    public ResponseEntity<?> updateDetails(@PathVariable Long labId,
                                           @Valid @RequestBody LabOnboardingDetailsDTO request) {
        return handle("Onboarding details updated successfully",
                user -> labOnboardingDetailsService.updateDetails(user, labId, request));
    }

    private ResponseEntity<?> handle(String successMessage, Function<User, LabOnboardingDetailsDTO> action) {
        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
        }
        try {
            LabOnboardingDetailsDTO result = action.apply(currentUserOptional.get());
            return ApiResponseHelper.successResponseWithDataAndMessage(successMessage, HttpStatus.OK, result);
        } catch (IllegalArgumentException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.BAD_REQUEST, null);
        } catch (NoSuchElementException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.NOT_FOUND, null);
        } catch (IllegalStateException e) {
            // Only thrown when the user is not a member of the lab
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.UNAUTHORIZED, null);
        } catch (Exception e) {
            log.error("Onboarding details operation failed and was rolled back: {}", e.getMessage(), e);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Onboarding details operation failed. No changes were saved.", HttpStatus.INTERNAL_SERVER_ERROR, null);
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
        if (principal instanceof String username && !"anonymousUser".equalsIgnoreCase(username)) {
            return userService.findByUsername(username);
        }
        return Optional.empty();
    }
}
