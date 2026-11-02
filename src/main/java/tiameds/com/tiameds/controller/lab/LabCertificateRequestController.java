package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.LabCertificateRequestService;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

/**
 * Adding or replacing a lab certificate. The request is reviewed by a platform admin; until it is approved the lab
 * keeps its current certificate, and no file is ever deleted.
 * Covered by the existing /lab/admin/** rule (ADMIN / SUPERADMIN); the user must also be a member of the lab.
 *
 * type values: organizationGstinCertificate, gstinCertificate (lab GST), establishmentRegistrationCertificate,
 * biomedicalWasteAuthorizationCertificate, nablAccreditationCertificate. The logo is not a certificate and is
 * still handled by /lab/admin/{labId}/documents.
 */
@Slf4j
@RestController
@RequestMapping("/lab/admin/{labId}/certificate-requests")
@Tag(name = "Lab Certificate Requests", description = "Request a new or replacement lab certificate for platform-admin approval")
public class LabCertificateRequestController {

    private final LabCertificateRequestService certificateRequestService;
    private final UserService userService;

    public LabCertificateRequestController(LabCertificateRequestService certificateRequestService,
                                           UserService userService) {
        this.certificateRequestService = certificateRequestService;
        this.userService = userService;
    }

    @Operation(summary = "Submit a certificate request",
            description = "Uploads the new certificate and creates a PENDING request. The current certificate stays active "
                    + "until the request is approved. issuingDate / expiryDate (yyyy-MM-dd) apply to the "
                    + "establishmentRegistrationCertificate only.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submit(@PathVariable Long labId,
                                    @RequestParam("type") String type,
                                    @RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "issuingDate", required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuingDate,
                                    @RequestParam(value = "expiryDate", required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate) {
        return handle("Certificate request submitted. It will take effect once it is approved.",
                user -> certificateRequestService.submit(user, labId, type, file, issuingDate, expiryDate));
    }

    @Operation(summary = "List certificate requests of the lab",
            description = "Newest first: pending, rejected (with reason) and approved requests. Approved ones carry the file they replaced.")
    @GetMapping
    public ResponseEntity<?> getRequests(@PathVariable Long labId) {
        return handle("Certificate requests fetched successfully",
                user -> certificateRequestService.getLabRequests(user, labId));
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
            HttpStatus status = e.getMessage() != null && e.getMessage().toLowerCase().contains("member")
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.INTERNAL_SERVER_ERROR;
            if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
                log.error("Certificate request operation failed: {}", e.getMessage(), e);
            }
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), status, null);
        } catch (Exception e) {
            log.error("Certificate request operation failed and was rolled back: {}", e.getMessage(), e);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Certificate request operation failed. No changes were saved.", HttpStatus.INTERNAL_SERVER_ERROR, null);
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
