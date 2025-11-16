package tiameds.com.tiameds.controller.sampleassociation;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.audit.AuditLogService;
import tiameds.com.tiameds.audit.helpers.FieldChangeTracker;
import tiameds.com.tiameds.dto.lab.SampleDto;
import tiameds.com.tiameds.entity.LabAuditLogs;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.SampleAssociationService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/lab")
@Tag(name = "Sample Association", description = "manage the sample associations in the lab")
public class SampleAssociation {

    private final UserService userService;
    private final SampleAssociationService sampleAssociationService;
    private final AuditLogService auditLogService;
    private final FieldChangeTracker fieldChangeTracker;
    private final LabAccessableFilter labAccessableFilter;


    public SampleAssociation(UserService userService,
                             SampleAssociationService sampleAssociationService,
                             AuditLogService auditLogService,
                             FieldChangeTracker fieldChangeTracker,
                             LabAccessableFilter labAccessableFilter) {
        this.userService = userService;
        this.sampleAssociationService = sampleAssociationService;
        this.auditLogService = auditLogService;
        this.fieldChangeTracker = fieldChangeTracker;
        this.labAccessableFilter = labAccessableFilter;
    }

    // Get all sample associations of a respective lab
    @GetMapping("/{labId}/sample-list")
    public ResponseEntity<?> getSampleAssociationList(@PathVariable Long labId){

        //check if the user is authenticated
        Optional<User> currentUser = getAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        // Validate lab access
        if (!labAccessableFilter.isLabAccessible(labId)) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        // Delegate to the service layer
        List<SampleDto> sampleAssociationList = sampleAssociationService.getSampleAssociationList(labId);

        return ApiResponseHelper.successResponse("Sample associations retrieved successfully", sampleAssociationList);
    }


    // create sample
    @PostMapping("/{labId}/sample")
    public ResponseEntity<?> createSample(
            @PathVariable Long labId,
            @RequestBody SampleDto sampleDto,
            HttpServletRequest request) {

        Optional<User> currentUser = getAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        // Validate lab access
        if (!labAccessableFilter.isLabAccessible(labId)) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        try {
            SampleDto createdSample = sampleAssociationService.createSample(sampleDto, labId);

            logSampleAudit(
                    "SAMPLE_CREATE",
                    null,
                    createdSample,
                    resolveChangeReason("created", createdSample.getName()),
                    currentUser.get(),
                    request,
                    createdSample.getId(),
                    labId
            );

            return ApiResponseHelper.successResponse("Sample created successfully", createdSample);
        } catch (RuntimeException e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    // update sample
    @PutMapping("/{labId}/sample/{sampleId}")
    public ResponseEntity<?> updateSample(
            @PathVariable Long labId,
            @PathVariable("sampleId") Long sampleId,
            @RequestBody SampleDto sampleDto,
            HttpServletRequest request) {

        try {
            // Authenticate user
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Validate lab access
            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            SampleDto oldSample = sampleAssociationService.getSampleSnapshot(sampleId, labId);
            if (oldSample == null) {
                return ApiResponseHelper.errorResponse("Sample not found", HttpStatus.NOT_FOUND);
            }

            // Delegate to the service layer
            SampleDto updatedSample = sampleAssociationService.updateSample(sampleId, sampleDto, labId);

            logSampleAudit(
                    "SAMPLE_UPDATE",
                    oldSample,
                    updatedSample,
                    resolveChangeReason("updated", updatedSample.getName()),
                    currentUser.get(),
                    request,
                    updatedSample.getId(),
                    labId
            );

            return ApiResponseHelper.successResponse("Sample updated successfully", updatedSample);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    // delete sample
    @DeleteMapping("/{labId}/sample/{sampleId}")
    public ResponseEntity<?> deleteSample(
            @PathVariable Long labId,
            @PathVariable("sampleId") Long sampleId,
            HttpServletRequest request) {

        try {
            // Authenticate user
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Validate lab access
            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            SampleDto oldSample = sampleAssociationService.getSampleSnapshot(sampleId, labId);
            if (oldSample == null) {
                return ApiResponseHelper.errorResponse("Sample not found", HttpStatus.NOT_FOUND);
            }

            // Delegate to the service layer
            sampleAssociationService.deleteSample(sampleId, labId);

            logSampleAudit(
                    "SAMPLE_DELETE",
                    oldSample,
                    null,
                    resolveChangeReason("deleted", oldSample.getName()),
                    currentUser.get(),
                    request,
                    oldSample.getId(),
                    labId
            );

            return ApiResponseHelper.successResponse("Sample deleted successfully", null);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }



    private void logSampleAudit(String action,
                               SampleDto oldSample,
                               SampleDto newSample,
                               String changeReason,
                               User currentUser,
                               HttpServletRequest request,
                               Long entityId,
                               Long labId) {
        LabAuditLogs auditLog = new LabAuditLogs();
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setModule("SampleAssociation");
        auditLog.setEntityType("Sample");
        auditLog.setLab_id(String.valueOf(labId));
        auditLog.setActionType(action);
        auditLog.setChangeReason(changeReason != null ? changeReason : "");

        if (entityId != null) {
            auditLog.setEntityId(String.valueOf(entityId));
        }

        if (currentUser != null) {
            auditLog.setUsername(currentUser.getUsername());
            auditLog.setUserId(currentUser.getId());
            if (currentUser.getRoles() != null && !currentUser.getRoles().isEmpty()) {
                auditLog.setRole(currentUser.getRoles().iterator().next().getName());
            }
        } else {
            auditLog.setUsername("system");
        }

        if (request != null) {
            String ipAddress = request.getHeader("X-Forwarded-For");
            auditLog.setIpAddress(ipAddress != null ? ipAddress : request.getRemoteAddr());
            auditLog.setDeviceInfo(request.getHeader("User-Agent"));
            auditLog.setRequestId(request.getHeader("X-Request-ID"));
        }

        auditLog.setOldValue(fieldChangeTracker.objectToJson(oldSample));
        auditLog.setNewValue(fieldChangeTracker.objectToJson(newSample));

        Map<String, Object> fieldChanges = fieldChangeTracker.compareSampleFields(oldSample, newSample);
        String fieldChangedJson = fieldChangeTracker.fieldChangesToJson(fieldChanges);
        if (fieldChangedJson != null) {
            auditLog.setFieldChanged(fieldChangedJson);
        }

        auditLog.setSeverity(LabAuditLogs.Severity.MEDIUM);
        auditLogService.persistAsync(auditLog);
    }

    private String resolveChangeReason(String action, String sampleName) {
        String name = sampleName != null ? sampleName : "sample";
        return switch (action) {
            case "created" -> "Created sample " + name;
            case "updated" -> "Updated sample " + name;
            case "deleted" -> "Deleted sample " + name;
            default -> name;
        };
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
}
