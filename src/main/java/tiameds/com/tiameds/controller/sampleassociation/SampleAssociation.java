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


    public SampleAssociation(UserService userService,
                             SampleAssociationService sampleAssociationService,
                             AuditLogService auditLogService,
                             FieldChangeTracker fieldChangeTracker) {
        this.userService = userService;
        this.sampleAssociationService = sampleAssociationService;
        this.auditLogService = auditLogService;
        this.fieldChangeTracker = fieldChangeTracker;
    }

    // Get all sample associations of a respective lab
    @GetMapping("/sample-list")
    public ResponseEntity<?> getSampleAssociationList(){

        //check if the user is authenticated
        Optional<User> currentUser = getAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        // Delegate to the service layer
        List<SampleDto> sampleAssociationList = sampleAssociationService.getSampleAssociationList();

        return ApiResponseHelper.successResponse("Sample associations retrieved successfully", sampleAssociationList);
    }


    // create sample
    @PostMapping("/sample")
    public ResponseEntity<?> createSample(
            @RequestBody SampleDto sampleDto,
            HttpServletRequest request) {

        Optional<User> currentUser = getAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        try {
            SampleDto createdSample = sampleAssociationService.createSample(sampleDto);

            logSampleAudit(
                    "SAMPLE_CREATE",
                    null,
                    createdSample,
                    resolveChangeReason("created", createdSample.getName()),
                    currentUser.get(),
                    request,
                    createdSample.getId()
            );

            return ApiResponseHelper.successResponse("Sample created successfully", createdSample);
        } catch (RuntimeException e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    // update sample
    @PutMapping("/sample/{sampleId}")
    public ResponseEntity<?> updateSample(
            @PathVariable("sampleId") Long sampleId,
            @RequestBody SampleDto sampleDto,
            HttpServletRequest request) {

        try {
            // Authenticate user
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            SampleDto oldSample = sampleAssociationService.getSampleSnapshot(sampleId);
            if (oldSample == null) {
                return ApiResponseHelper.errorResponse("Sample not found", HttpStatus.NOT_FOUND);
            }

            // Delegate to the service layer
            SampleDto updatedSample = sampleAssociationService.updateSample(sampleId, sampleDto);

            logSampleAudit(
                    "SAMPLE_UPDATE",
                    oldSample,
                    updatedSample,
                    resolveChangeReason("updated", updatedSample.getName()),
                    currentUser.get(),
                    request,
                    updatedSample.getId()
            );

            return ApiResponseHelper.successResponse("Sample updated successfully", updatedSample);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    // delete sample
    @DeleteMapping("/sample/{sampleId}")
    public ResponseEntity<?> deleteSample(
            @PathVariable("sampleId") Long sampleId,
            HttpServletRequest request) {

        try {
            // Authenticate user
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            SampleDto oldSample = sampleAssociationService.getSampleSnapshot(sampleId);
            if (oldSample == null) {
                return ApiResponseHelper.errorResponse("Sample not found", HttpStatus.NOT_FOUND);
            }

            // Delegate to the service layer
            sampleAssociationService.deleteSample(sampleId);

            logSampleAudit(
                    "SAMPLE_DELETE",
                    oldSample,
                    null,
                    resolveChangeReason("deleted", oldSample.getName()),
                    currentUser.get(),
                    request,
                    oldSample.getId()
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
                               Long entityId) {
        LabAuditLogs auditLog = new LabAuditLogs();
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setModule("SampleAssociation");
        auditLog.setEntityType("Sample");
        auditLog.setLab_id("GLOBAL");
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
