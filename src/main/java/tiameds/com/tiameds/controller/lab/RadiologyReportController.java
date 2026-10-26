package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.audit.AuditLogService;
import tiameds.com.tiameds.audit.helpers.FieldChangeTracker;
import tiameds.com.tiameds.dto.lab.RadiologyFinalizeRequest;
import tiameds.com.tiameds.dto.lab.RadiologyReportRequest;
import tiameds.com.tiameds.dto.lab.RadiologyTemplateRequest;
import tiameds.com.tiameds.entity.LabAuditLogs;
import tiameds.com.tiameds.entity.RadiologyReportFormat;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.RadiologyReportService;
import tiameds.com.tiameds.services.lab.RadiologyReportFormatService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/lab")
@Tag(name = "Radiology Report", description = "Radiology (imaging) templates, reports, signing doctors and amendment history")
public class RadiologyReportController {

    private static final Set<String> WRITE_ROLES = Set.of("ADMIN", "TECHNICIAN", "SUPERADMIN");
    private static final Set<String> TEMPLATE_ROLES = Set.of("ADMIN", "SUPERADMIN");

    private final RadiologyReportService radiologyReportService;
    private final RadiologyReportFormatService radiologyReportFormatService;
    private final LabAccessableFilter labAccessableFilter;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final FieldChangeTracker fieldChangeTracker;

    public RadiologyReportController(RadiologyReportService radiologyReportService,
                                     RadiologyReportFormatService radiologyReportFormatService,
                                     LabAccessableFilter labAccessableFilter,
                                     UserService userService,
                                     AuditLogService auditLogService,
                                     FieldChangeTracker fieldChangeTracker) {
        this.radiologyReportService = radiologyReportService;
        this.radiologyReportFormatService = radiologyReportFormatService;
        this.labAccessableFilter = labAccessableFilter;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.fieldChangeTracker = fieldChangeTracker;
    }

    // ------------------------------------------------------------------ templates

    @Operation(summary = "Create a new version of a radiology template", tags = {"Radiology Report"})
    @PostMapping("/{labId}/radiology/templates")
    public ResponseEntity<?> createTemplate(
            @PathVariable Long labId,
            @RequestBody RadiologyTemplateRequest request,
            HttpServletRequest httpRequest) {
        Optional<User> currentUser = getAuthenticatedUser();
        ResponseEntity<?> denied = checkAccess(currentUser, labId, TEMPLATE_ROLES);
        if (denied != null) {
            return denied;
        }
        if (Boolean.TRUE.equals(request.getGlobal()) && !hasAnyRole(Set.of("SUPERADMIN"))) {
            return ApiResponseHelper.errorResponse("Only a super admin can create a global template", HttpStatus.FORBIDDEN);
        }
        try {
            Map<String, Object> created = radiologyReportService.createTemplate(labId, request, currentUser.get().getUsername());
            logAudit(labId, "RADIOLOGY_TEMPLATE_CREATE", "RadiologyTemplate", String.valueOf(created.get("id")),
                    Map.of("name", String.valueOf(created.get("name")), "version", created.get("version")),
                    "Created radiology template version", currentUser.get(), httpRequest);
            return ApiResponseHelper.successResponse("Radiology template created successfully", created);
        } catch (Exception e) {
            return handle(e);
        }
    }

    @Operation(summary = "List active radiology templates available to the lab", tags = {"Radiology Report"})
    @GetMapping("/{labId}/radiology/templates")
    public ResponseEntity<?> listTemplates(
            @PathVariable Long labId,
            @RequestParam(required = false) Long testId) {
        Optional<User> currentUser = getAuthenticatedUser();
        ResponseEntity<?> denied = checkAccess(currentUser, labId, null);
        if (denied != null) {
            return denied;
        }
        try {
            return ApiResponseHelper.successResponse("Radiology templates fetched successfully",
                    radiologyReportService.listTemplates(labId, testId));
        } catch (Exception e) {
            return handle(e);
        }
    }

    // -------------------------------------------------------------------- reports

    @Operation(summary = "Save (create/update/amend) the radiology report of a test in a visit", tags = {"Radiology Report"})
    @PutMapping("/{labId}/radiology/visit/{visitId}/test/{testId}/report")
    public ResponseEntity<?> saveReport(
            @PathVariable Long labId,
            @PathVariable Long visitId,
            @PathVariable Long testId,
            @RequestBody RadiologyReportRequest request,
            HttpServletRequest httpRequest) {
        Optional<User> currentUser = getAuthenticatedUser();
        ResponseEntity<?> denied = checkAccess(currentUser, labId, WRITE_ROLES);
        if (denied != null) {
            return denied;
        }
        try {
            Map<String, Object> saved = radiologyReportService.saveReport(
                    labId, visitId, testId, request, currentUser.get().getUsername());
            boolean amended = "AMENDED".equals(String.valueOf(saved.get("status")));
            logAudit(labId, amended ? "RADIOLOGY_REPORT_AMEND" : "RADIOLOGY_REPORT_SAVE", "RadiologyReport",
                    String.valueOf(saved.get("reportCode")), statusSummary(saved),
                    amended ? request.getReason() : "Saved radiology report for visit " + visitId,
                    currentUser.get(), httpRequest);
            return ApiResponseHelper.successResponse("Radiology report saved successfully", saved);
        } catch (Exception e) {
            return handle(e);
        }
    }

    @Operation(summary = "Finalize (sign off) a radiology report", tags = {"Radiology Report"})
    @PostMapping("/{labId}/radiology/visit/{visitId}/test/{testId}/report/finalize")
    public ResponseEntity<?> finalizeReport(
            @PathVariable Long labId,
            @PathVariable Long visitId,
            @PathVariable Long testId,
            @RequestBody(required = false) RadiologyFinalizeRequest request,
            HttpServletRequest httpRequest) {
        Optional<User> currentUser = getAuthenticatedUser();
        ResponseEntity<?> denied = checkAccess(currentUser, labId, WRITE_ROLES);
        if (denied != null) {
            return denied;
        }
        try {
            Map<String, Object> finalized = radiologyReportService.finalizeReport(
                    labId, visitId, testId, request != null ? request.getVersion() : null,
                    currentUser.get().getUsername());
            logAudit(labId, "RADIOLOGY_REPORT_FINALIZE", "RadiologyReport",
                    String.valueOf(finalized.get("reportCode")), statusSummary(finalized),
                    "Finalized radiology report for visit " + visitId, currentUser.get(), httpRequest);
            return ApiResponseHelper.successResponse("Radiology report finalized successfully", finalized);
        } catch (Exception e) {
            return handle(e);
        }
    }

    @Operation(summary = "Get the radiology report of a test in a visit", tags = {"Radiology Report"})
    @GetMapping("/{labId}/radiology/visit/{visitId}/test/{testId}/report")
    public ResponseEntity<?> getReport(
            @PathVariable Long labId,
            @PathVariable Long visitId,
            @PathVariable Long testId) {
        Optional<User> currentUser = getAuthenticatedUser();
        ResponseEntity<?> denied = checkAccess(currentUser, labId, null);
        if (denied != null) {
            return denied;
        }
        try {
            return radiologyReportService.getReport(labId, visitId, testId)
                    .map(report -> ApiResponseHelper.successResponse("Radiology report fetched successfully", report))
                    .orElseGet(() -> ApiResponseHelper.successResponse("No radiology report found", null));
        } catch (Exception e) {
            return handle(e);
        }
    }

    @Operation(summary = "List all radiology reports of a visit", tags = {"Radiology Report"})
    @GetMapping("/{labId}/radiology/visit/{visitId}/reports")
    public ResponseEntity<?> listReportsForVisit(
            @PathVariable Long labId,
            @PathVariable Long visitId) {
        Optional<User> currentUser = getAuthenticatedUser();
        ResponseEntity<?> denied = checkAccess(currentUser, labId, null);
        if (denied != null) {
            return denied;
        }
        try {
            return ApiResponseHelper.successResponse("Radiology reports fetched successfully",
                    radiologyReportService.listReportsForVisit(labId, visitId));
        } catch (Exception e) {
            return handle(e);
        }
    }

    @Operation(summary = "Get the amendment history of a radiology report", tags = {"Radiology Report"})
    @GetMapping("/{labId}/radiology/visit/{visitId}/test/{testId}/report/history")
    public ResponseEntity<?> getHistory(
            @PathVariable Long labId,
            @PathVariable Long visitId,
            @PathVariable Long testId) {
        Optional<User> currentUser = getAuthenticatedUser();
        ResponseEntity<?> denied = checkAccess(currentUser, labId, null);
        if (denied != null) {
            return denied;
        }
        try {
            return ApiResponseHelper.successResponse("Radiology report history fetched successfully",
                    radiologyReportService.getHistory(labId, visitId, testId));
        } catch (Exception e) {
            return handle(e);
        }
    }

    // -------------------------------------------------------------- report formats

    @Operation(summary = "Upload a Word report format (.doc / .docx) with its document name and doctor name", tags = {"Radiology Report"})
    @PostMapping(value = "/{labId}/radiology/report-formats", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadReportFormat(
            @PathVariable Long labId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentName", required = false) String documentName,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            @RequestParam(value = "doctorName", required = false) String doctorName,
            HttpServletRequest httpRequest) {
        Optional<User> currentUser = getAuthenticatedUser();
        ResponseEntity<?> denied = checkAccess(currentUser, labId, WRITE_ROLES);
        if (denied != null) {
            return denied;
        }
        try {
            Map<String, Object> saved = radiologyReportFormatService.upload(
                    labId, file, documentName, doctorId, doctorName, currentUser.get().getUsername());
            logAudit(labId, "RADIOLOGY_FORMAT_UPLOAD", "RadiologyReportFormat", String.valueOf(saved.get("id")),
                    Map.of("documentName", String.valueOf(saved.get("documentName")),
                            "doctorName", String.valueOf(saved.get("doctorName"))),
                    "Uploaded radiology report format", currentUser.get(), httpRequest);
            return ApiResponseHelper.successResponse("Report format saved successfully", saved);
        } catch (Exception e) {
            return handle(e);
        }
    }

    @Operation(summary = "List the Word report formats uploaded for the lab", tags = {"Radiology Report"})
    @GetMapping("/{labId}/radiology/report-formats")
    public ResponseEntity<?> listReportFormats(@PathVariable Long labId) {
        Optional<User> currentUser = getAuthenticatedUser();
        ResponseEntity<?> denied = checkAccess(currentUser, labId, null);
        if (denied != null) {
            return denied;
        }
        try {
            return ApiResponseHelper.successResponse("Report formats fetched successfully",
                    radiologyReportFormatService.list(labId));
        } catch (Exception e) {
            return handle(e);
        }
    }

    @Operation(summary = "Get an uploaded Word report format as JSON (text and tables)", tags = {"Radiology Report"})
    @GetMapping("/{labId}/radiology/report-formats/{id}")
    public ResponseEntity<?> getReportFormat(@PathVariable Long labId, @PathVariable Long id) {
        Optional<User> currentUser = getAuthenticatedUser();
        ResponseEntity<?> denied = checkAccess(currentUser, labId, null);
        if (denied != null) {
            return denied;
        }
        try {
            return ApiResponseHelper.successResponse("Report format fetched successfully",
                    radiologyReportFormatService.getContent(labId, id));
        } catch (Exception e) {
            return handle(e);
        }
    }

    @Operation(summary = "Download an uploaded Word report format", tags = {"Radiology Report"})
    @GetMapping("/{labId}/radiology/report-formats/{id}/download")
    public ResponseEntity<?> downloadReportFormat(@PathVariable Long labId, @PathVariable Long id) {
        Optional<User> currentUser = getAuthenticatedUser();
        ResponseEntity<?> denied = checkAccess(currentUser, labId, null);
        if (denied != null) {
            return denied;
        }
        try {
            RadiologyReportFormat format = radiologyReportFormatService.get(labId, id);
            String contentType = format.getContentType() != null && !format.getContentType().isBlank()
                    ? format.getContentType() : "application/octet-stream";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(format.getDocumentName(), StandardCharsets.UTF_8).build().toString())
                    .contentLength(format.getFileData().length)
                    .body(format.getFileData());
        } catch (Exception e) {
            return handle(e);
        }
    }

    // -------------------------------------------------------------------- helpers

    // Returns an error response when access is denied, null when the call may proceed.
    // allowedRoles == null means any lab member may call it.
    private ResponseEntity<?> checkAccess(Optional<User> currentUser, Long labId, Set<String> allowedRoles) {
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }
        if (!labAccessableFilter.isLabAccessible(labId)) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }
        if (!userService.isUserMemberOfLab(currentUser.get().getId(), labId)) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }
        if (allowedRoles != null && !hasAnyRole(allowedRoles)) {
            return ApiResponseHelper.errorResponse("You do not have permission to perform this action", HttpStatus.FORBIDDEN);
        }
        return null;
    }

    private boolean hasAnyRole(Set<String> roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String name = authority.getAuthority();
            if (name != null && name.startsWith("ROLE_") && roles.contains(name.substring(5))) {
                return true;
            }
        }
        return false;
    }

    private ResponseEntity<?> handle(Exception e) {
        if (e instanceof IllegalArgumentException) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        if (e instanceof NoSuchElementException) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
        if (e instanceof IllegalStateException) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.CONFLICT);
        }
        if (e instanceof ObjectOptimisticLockingFailureException) {
            return ApiResponseHelper.errorResponse("Report was changed by someone else. Reload it and try again.", HttpStatus.CONFLICT);
        }
        if (e instanceof DataIntegrityViolationException) {
            log.warn("Radiology data conflict: {}", e.getMessage());
            return ApiResponseHelper.errorResponse("The record was created or changed at the same time. Reload and try again.", HttpStatus.CONFLICT);
        }
        log.error("Unexpected radiology report error", e);
        return ApiResponseHelper.errorResponse("Unexpected error while processing the radiology report", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Only metadata goes to the audit log; the report body stays in radiology_report / history.
    private Map<String, Object> statusSummary(Map<String, Object> report) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("reportCode", report.get("reportCode"));
        summary.put("status", String.valueOf(report.get("status")));
        summary.put("version", report.get("version"));
        summary.put("visitId", report.get("visitId"));
        summary.put("testId", report.get("testId"));
        return summary;
    }

    private void logAudit(Long labId, String action, String entityType, String entityId,
                          Map<String, Object> newData, String reason, User user, HttpServletRequest request) {
        try {
            LabAuditLogs auditLog = new LabAuditLogs();
            auditLog.setTimestamp(LocalDateTime.now());
            auditLog.setModule("Radiology");
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setLab_id(String.valueOf(labId));
            auditLog.setActionType(action);
            auditLog.setChangeReason(reason != null ? reason : "");
            auditLog.setUsername(user.getUsername());
            auditLog.setUserId(user.getId());
            List<String> roles = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a != null && a.startsWith("ROLE_"))
                    .map(a -> a.substring(5))
                    .collect(Collectors.toList());
            if (!roles.isEmpty()) {
                auditLog.setRole(roles.get(0));
            }
            if (request != null) {
                String ipAddress = request.getHeader("X-Forwarded-For");
                auditLog.setIpAddress(ipAddress != null ? ipAddress : request.getRemoteAddr());
                auditLog.setDeviceInfo(request.getHeader("User-Agent"));
                auditLog.setRequestId(request.getHeader("X-Request-ID"));
            }
            auditLog.setNewValue(fieldChangeTracker.objectToJson(newData));
            auditLog.setSeverity(LabAuditLogs.Severity.MEDIUM);
            auditLogService.persistAsync(auditLog);
        } catch (Exception ex) {
            // Auditing must never fail the clinical operation that already succeeded.
            log.warn("Could not write radiology audit log for {}: {}", action, ex.getMessage());
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
