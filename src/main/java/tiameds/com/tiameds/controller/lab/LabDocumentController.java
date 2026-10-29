package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
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
import tiameds.com.tiameds.services.lab.LabDocumentService;
import tiameds.com.tiameds.services.lab.LabDocumentType;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Lab logo and certificate files stored in S3 (URL stored on the lab).
 * Covered by the existing /lab/admin/** rule (ADMIN / SUPERADMIN).
 *
 * Part names / type values: labLogo, organizationGstinCertificate, gstinCertificate (lab GST), establishmentRegistrationCertificate,
 * biomedicalWasteAuthorizationCertificate, nablAccreditationCertificate.
 */
@Slf4j
@RestController
@RequestMapping("/lab/admin/{labId}/documents")
@Tag(name = "Lab Documents", description = "Add, update and delete lab logo and certificates stored in S3")
public class LabDocumentController {

    private final LabDocumentService labDocumentService;
    private final UserService userService;

    public LabDocumentController(LabDocumentService labDocumentService, UserService userService) {
        this.labDocumentService = labDocumentService;
        this.userService = userService;
    }

    @Operation(summary = "Get lab logo / certificate URLs",
            description = "Returns the stored S3 URL for the logo and each certificate (null if not uploaded).")
    @GetMapping
    public ResponseEntity<?> getDocuments(@PathVariable Long labId) {
        return handle("Documents fetched successfully", user -> labDocumentService.getDocuments(user, labId));
    }

    @Operation(summary = "Add lab logo / certificates",
            description = "Uploads files to S3 and stores their URLs. Fails if a document already exists. All-or-nothing.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addDocuments(@PathVariable Long labId,
                                          @RequestParam Map<String, MultipartFile> files) {
        return handle("Documents added successfully",
                user -> labDocumentService.addDocuments(user, labId, toTypedFiles(files)));
    }

    @Operation(summary = "Update lab logo / certificates",
            description = "Replaces existing documents in S3 and the stored URLs. Old files are deleted after commit. All-or-nothing.")
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateDocuments(@PathVariable Long labId,
                                             @RequestParam Map<String, MultipartFile> files) {
        return handle("Documents updated successfully",
                user -> labDocumentService.updateDocuments(user, labId, toTypedFiles(files)));
    }

    @Operation(summary = "Delete lab logo / certificates",
            description = "Clears the stored URLs and deletes the S3 objects after commit. All-or-nothing.")
    @DeleteMapping
    public ResponseEntity<?> deleteDocuments(@PathVariable Long labId,
                                             @RequestParam("types") List<String> types) {
        return handle("Documents deleted successfully", user -> {
            Set<LabDocumentType> typeSet = new LinkedHashSet<>();
            types.forEach(type -> typeSet.add(LabDocumentType.fromParam(type)));
            return labDocumentService.deleteDocuments(user, labId, typeSet);
        });
    }

    private Map<LabDocumentType, MultipartFile> toTypedFiles(Map<String, MultipartFile> files) {
        Map<LabDocumentType, MultipartFile> typed = new LinkedHashMap<>();
        files.forEach((name, file) -> typed.put(LabDocumentType.fromParam(name), file));
        return typed;
    }

    private ResponseEntity<?> handle(String successMessage, Function<User, Map<String, String>> action) {
        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
        }
        try {
            Map<String, String> result = action.apply(currentUserOptional.get());
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
                log.error("Lab document operation failed: {}", e.getMessage(), e);
            }
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), status, null);
        } catch (Exception e) {
            log.error("Lab document operation failed and was rolled back: {}", e.getMessage(), e);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Document operation failed. No changes were saved.", HttpStatus.INTERNAL_SERVER_ERROR, null);
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
