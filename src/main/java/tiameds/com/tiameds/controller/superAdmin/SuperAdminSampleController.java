package tiameds.com.tiameds.controller.superAdmin;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.VisitSampleRepository;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.UserAuthService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/lab-super-admin/samples")
@Tag(name = "Super Admin Sample Controller", description = "Sample collection statistics for super admin")
public class SuperAdminSampleController {

    private final VisitSampleRepository visitSampleRepository;
    private final LabRepository labRepository;
    private final UserAuthService userAuthService;

    public SuperAdminSampleController(VisitSampleRepository visitSampleRepository,
                                      LabRepository labRepository,
                                      UserAuthService userAuthService) {
        this.visitSampleRepository = visitSampleRepository;
        this.labRepository = labRepository;
        this.userAuthService = userAuthService;
    }

    @GetMapping("/{labId}/collected")
    public ResponseEntity<?> getCollectedSamples(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        long totalCollected;
        long collectedBySuperAdmin;
        long collectedByAdmin;
        long collectedByTechnician;

        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end   = endDate.atTime(LocalTime.MAX);

            totalCollected        = visitSampleRepository.countCollectedSamplesByLabIdAndCreatedAtBetween(labId, start, end);
            collectedBySuperAdmin = visitSampleRepository.countCollectedSamplesByLabIdAndRoleAndCreatedAtBetween(labId, "SUPERADMIN", start, end);
            collectedByAdmin      = visitSampleRepository.countCollectedSamplesByLabIdAndRoleAndCreatedAtBetween(labId, "ADMIN", start, end);
            collectedByTechnician = visitSampleRepository.countCollectedSamplesByLabIdAndRoleAndCreatedAtBetween(labId, "TECHNICIAN", start, end);
        } else {
            totalCollected        = visitSampleRepository.countCollectedSamplesByLabId(labId);
            collectedBySuperAdmin = visitSampleRepository.countCollectedSamplesByLabIdAndRole(labId, "SUPERADMIN");
            collectedByAdmin      = visitSampleRepository.countCollectedSamplesByLabIdAndRole(labId, "ADMIN");
            collectedByTechnician = visitSampleRepository.countCollectedSamplesByLabIdAndRole(labId, "TECHNICIAN");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("labId", labId);
        response.put("labName", labOptional.get().getName());
        response.put("totalCollected", totalCollected);
        response.put("collectedBySuperAdmin", collectedBySuperAdmin);
        response.put("collectedByAdmin", collectedByAdmin);
        response.put("collectedByTechnician", collectedByTechnician);
        return ApiResponseHelper.successResponse("Collected samples retrieved successfully", response);
    }
}
