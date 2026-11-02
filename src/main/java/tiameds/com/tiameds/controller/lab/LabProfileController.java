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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tiameds.com.tiameds.dto.auth.UserProfileUpdateDTO;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.LabProfileService;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

/**
 * Profile screen after login.
 * GET /lab/admin/profile is covered by the existing /lab/admin/** rule (ADMIN / SUPERADMIN).
 * PUT /lab/admin/me is covered by the existing /lab/admin/me rule (every role), the same rule as GET /lab/admin/me.
 */
@Slf4j
@RestController
@RequestMapping("/lab/admin")
@Tag(name = "Profile", description = "View the profile (user, lab, certificates) and edit personal details")
public class LabProfileController {

    private final LabProfileService labProfileService;
    private final UserService userService;

    public LabProfileController(LabProfileService labProfileService, UserService userService) {
        this.labProfileService = labProfileService;
        this.userService = userService;
    }

    @Operation(summary = "Get profile",
            description = "Returns the user's details and, for each lab the user belongs to (or only labId if given), the "
                    + "organization / lab details, the live certificate URLs and the certificate request history.")
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam(value = "labId", required = false) Long labId) {
        return handle("Profile fetched successfully", user -> labProfileService.getProfile(user, labId));
    }

    @Operation(summary = "Edit my personal details",
            description = "Updates the logged-in user's own firstName, lastName, phone, address, city, state, zip and country. "
                    + "Only the fields that are sent change. Username, email, password and roles cannot be changed here.")
    @PutMapping("/me")
    public ResponseEntity<?> updateMyDetails(@Valid @RequestBody UserProfileUpdateDTO request) {
        return handle("Personal details updated successfully", user -> labProfileService.updateMyDetails(user, request));
    }

    private ResponseEntity<?> handle(String successMessage, Function<User, Object> action) {
        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
        }
        try {
            Object result = action.apply(currentUserOptional.get());
            return ApiResponseHelper.successResponseWithDataAndMessage(successMessage, HttpStatus.OK, result);
        } catch (IllegalArgumentException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.BAD_REQUEST, null);
        } catch (NoSuchElementException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.NOT_FOUND, null);
        } catch (IllegalStateException e) {
            // Only thrown when the user is not a member of the requested lab
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.UNAUTHORIZED, null);
        } catch (Exception e) {
            log.error("Profile operation failed and was rolled back: {}", e.getMessage(), e);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Profile operation failed. No changes were saved.", HttpStatus.INTERNAL_SERVER_ERROR, null);
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
