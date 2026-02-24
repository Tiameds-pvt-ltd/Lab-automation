package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.*;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.ReportSettingsService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.repository.UserRepository;

import java.util.Optional;

@RestController
@RequestMapping("/lab/{labId}/report-settings")
@Tag(name = "Report Settings", description = "Configure lab report template, header, typography, signatures, and disclaimer")
public class ReportSettingController {

    private final ReportSettingsService reportSettingsService;
    private final UserService userService;
    private final UserRepository userRepository;

    public ReportSettingController(ReportSettingsService reportSettingsService,
                                  UserService userService,
                                  UserRepository userRepository) {
        this.reportSettingsService = reportSettingsService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof MyUserDetails myUserDetails) {
            return userService.findByUsername(myUserDetails.getUsername());
        }
        if (principal instanceof UserDetails userDetails) {
            return userService.findByUsername(userDetails.getUsername());
        }
        if (principal instanceof String username && !"anonymousUser".equalsIgnoreCase(username)) {
            return userService.findByUsername(username);
        }
        return Optional.empty();
    }

    private ResponseEntity<?> requireLabMember(Long labId) {
        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
        }
        User currentUser = currentUserOptional.get();
        if (!userRepository.existsByIdAndLabsId(currentUser.getId(), labId)) {
            return ApiResponseHelper.successResponseWithDataAndMessage("User is not a member of this lab", HttpStatus.UNAUTHORIZED, null);
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> getReportSettings(@PathVariable Long labId) {
        ResponseEntity<?> authFailure = requireLabMember(labId);
        if (authFailure != null) return authFailure;

        return reportSettingsService.getByLabId(labId)
                .map(data -> ApiResponseHelper.successResponseWithDataAndMessage("Report settings fetched successfully", HttpStatus.OK, data))
                .orElse(ApiResponseHelper.errorResponseWithMessage("Report settings not found for this lab", HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<?> createReportSettings(@PathVariable Long labId, @RequestBody ReportSettingsPayloadDTO payload) {
        ResponseEntity<?> authFailure = requireLabMember(labId);
        if (authFailure != null) return authFailure;

        try {
            ReportSettingsResponseDTO data = reportSettingsService.create(labId, payload);
            return ApiResponseHelper.successResponseWithDataAndMessage("Report settings saved successfully", HttpStatus.CREATED, data);
        } catch (IllegalArgumentException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.BAD_REQUEST, null);
        } catch (IllegalStateException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.CONFLICT, null);
        }
    }

    @PutMapping
    public ResponseEntity<?> updateReportSettings(@PathVariable Long labId, @RequestBody ReportSettingsPayloadDTO payload) {
        ResponseEntity<?> authFailure = requireLabMember(labId);
        if (authFailure != null) return authFailure;

        try {
            ReportSettingsResponseDTO data = reportSettingsService.update(labId, payload);
            return ApiResponseHelper.successResponseWithDataAndMessage("Report settings updated successfully", HttpStatus.OK, data);
        } catch (IllegalArgumentException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.BAD_REQUEST, null);
        }
    }

    @PostMapping("/signature/upload-url")
    public ResponseEntity<?> getSignatureUploadUrl(@PathVariable Long labId, @RequestBody ReportSignatureUploadRequestDTO request) {
        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
        }
        try {
            ReportSignatureUploadResponseDTO response = reportSettingsService.createSignatureUploadUrl(currentUserOptional.get(), labId, request);
            return ApiResponseHelper.successResponseWithDataAndMessage("Upload URL generated", HttpStatus.OK, response);
        } catch (IllegalArgumentException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.BAD_REQUEST, null);
        } catch (IllegalStateException e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().toLowerCase().contains("configured")
                    ? HttpStatus.INTERNAL_SERVER_ERROR
                    : HttpStatus.UNAUTHORIZED;
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), status, null);
        }
    }
}
