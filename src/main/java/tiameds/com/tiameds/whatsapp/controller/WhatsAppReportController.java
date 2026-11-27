package tiameds.com.tiameds.whatsapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tiameds.com.tiameds.audit.Auditable;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.whatsapp.dto.WhatsAppReportResponse;
import tiameds.com.tiameds.whatsapp.entity.WhatsAppReportLog;
import tiameds.com.tiameds.whatsapp.entity.WhatsAppSendStatus;
import tiameds.com.tiameds.whatsapp.service.WhatsAppReportService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/lab")
@Tag(name = "WhatsApp Report", description = "Send a visit's lab report PDF to the patient on WhatsApp")
public class WhatsAppReportController {

    // same roles that may generate reports (see SpringSecurityConfig: /lab/*/report/**, complete-visit)
    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_ADMIN", "ROLE_TECHNICIAN", "ROLE_SUPERADMIN");

    private final WhatsAppReportService service;
    private final LabAccessableFilter labAccessableFilter;
    private final UserService userService;

    public WhatsAppReportController(WhatsAppReportService service,
                                    LabAccessableFilter labAccessableFilter,
                                    UserService userService) {
        this.service = service;
        this.labAccessableFilter = labAccessableFilter;
        this.userService = userService;
    }

    @Auditable(module = "Lab", action = "SEND_WHATSAPP_REPORT")
    @Operation(summary = "Send the visit's lab report PDF(s) to the patient on WhatsApp",
            description = "Repeat the multipart part `file` to send several PDFs at once (max 10, each its own message). "
                    + "One file returns a single result object (as before); several files return a list of results.")
    @PostMapping(value = "/{labId}/visits/{visitId}/whatsapp-report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendReport(@PathVariable Long labId,
                                        @PathVariable Long visitId,
                                        @RequestPart("file") List<MultipartFile> files) {
        ResponseEntity<?> denied = checkAccess(labId);
        if (denied != null) {
            return denied;
        }
        try {
            User user = getAuthenticatedUser().orElseThrow();
            List<WhatsAppReportLog> entries = service.sendVisitReports(labId, visitId, files, user);
            List<WhatsAppReportResponse> rows = entries.stream().map(WhatsAppReportResponse::from).toList();
            long failed = entries.stream().filter(e -> e.getStatus() == WhatsAppSendStatus.FAILED).count();

            if (entries.size() == 1) {
                if (failed > 0) {
                    return ApiResponseHelper.successResponseWithDataAndMessage(
                            "WhatsApp message could not be sent", HttpStatus.BAD_GATEWAY, rows.get(0));
                }
                return ApiResponseHelper.successResponseWithDataAndMessage(
                        "Report sent on WhatsApp", HttpStatus.OK, rows.get(0));
            }
            if (failed == entries.size()) {
                return ApiResponseHelper.successResponseWithDataAndMessage(
                        "WhatsApp messages could not be sent", HttpStatus.BAD_GATEWAY, rows);
            }
            if (failed > 0) {
                return ApiResponseHelper.successResponseWithDataAndMessage(
                        (entries.size() - failed) + " of " + entries.size() + " reports sent on WhatsApp",
                        HttpStatus.MULTI_STATUS, rows);
            }
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    entries.size() + " reports sent on WhatsApp", HttpStatus.OK, rows);
        } catch (ResponseStatusException e) {
            return ApiResponseHelper.errorResponse(e.getReason(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("Could not send the report on WhatsApp", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Auditable(module = "Lab")
    @Operation(summary = "WhatsApp delivery history of a visit's report")
    @GetMapping("/{labId}/visits/{visitId}/whatsapp-report")
    public ResponseEntity<?> history(@PathVariable Long labId, @PathVariable Long visitId) {
        ResponseEntity<?> denied = checkAccess(labId);
        if (denied != null) {
            return denied;
        }
        try {
            List<WhatsAppReportResponse> rows = service.history(labId, visitId).stream()
                    .map(WhatsAppReportResponse::from).toList();
            return ApiResponseHelper.successResponseWithDataAndMessage("WhatsApp history fetched", HttpStatus.OK, rows);
        } catch (ResponseStatusException e) {
            return ApiResponseHelper.errorResponse(e.getReason(), HttpStatus.valueOf(e.getStatusCode().value()));
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("Could not fetch WhatsApp history", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Role + authenticated user + active lab + membership. Returns null when everything is fine. */
    private ResponseEntity<?> checkAccess(Long labId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).noneMatch(ALLOWED_ROLES::contains)) {
            return ApiResponseHelper.errorResponse("You are not allowed to send reports", HttpStatus.FORBIDDEN);
        }
        Optional<User> user = getAuthenticatedUser();
        if (user.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }
        if (!labAccessableFilter.isLabAccessible(labId)) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }
        if (!userService.isUserMemberOfLab(user.get().getId(), labId)) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }
        return null;
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
