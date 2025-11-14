package tiameds.com.tiameds.controller.lab;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tiameds.com.tiameds.audit.Auditable;
import tiameds.com.tiameds.dto.lab.PatientDetailsDto;
import tiameds.com.tiameds.dto.lab.VisitDTO;
import tiameds.com.tiameds.dto.visits.PatientVisitDTO;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.BillingService;
import tiameds.com.tiameds.services.lab.VisitService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Transactional
@RestController
@RequestMapping("/lab")
@Tag(name = "Visit Controller", description = "mannage the patient visit in the repective controller")
public class VisitController {
    private final VisitService visitService;
    private final LabAccessableFilter labAccessableFilter;
    private final UserService userService;

    public VisitController(VisitService visitService, BillingService billingService, LabAccessableFilter labAccessableFilter, UserService userService) {
        this.visitService = visitService;
        this.labAccessableFilter = labAccessableFilter;
        this.userService = userService;
    }

    @Auditable(module = "Lab")
    @PostMapping("/{labId}/add-visit/{patientId}")
    public ResponseEntity<?> addVisit(
            @PathVariable Long labId,
            @PathVariable Long patientId,
            @RequestBody VisitDTO visitDTO
    ) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            visitService.addVisit(labId, patientId, visitDTO, currentUser);
            return ApiResponseHelper.successResponse("Visit added successfully", HttpStatus.OK);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Auditable(module = "Lab")
    @GetMapping("/{labId}/visits")
    public ResponseEntity<?> getVisits(
            @PathVariable Long labId
    ) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
            }
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Lab is not accessible", HttpStatus.UNAUTHORIZED, null);
            }
            Object visits = visitService.getVisits(labId, currentUser);
            return ApiResponseHelper.successResponseWithDataAndMessage("Visits fetched successfully", HttpStatus.OK, visits);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Auditable(module = "Lab")
    @PutMapping("/{labId}/update-visit/{visitId}")
    public ResponseEntity<?> updateVisit(
            @PathVariable Long labId,
            @PathVariable Long visitId,
            @RequestBody VisitDTO visitDTO
    ) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            visitService.updateVisit(labId, visitId, visitDTO, currentUser);
            return ApiResponseHelper.successResponse("Visit updated successfully", HttpStatus.OK);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Auditable(module = "Lab")
    @DeleteMapping("/{labId}/delete-visit/{visitId}")
    public ResponseEntity<?> deleteVisit(
            @PathVariable Long labId,
            @PathVariable Long visitId
    ) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            visitService.deleteVisit(labId, visitId, currentUser);
            return ApiResponseHelper.successResponse("Visit deleted successfully", HttpStatus.OK);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Auditable(module = "Lab")
    @GetMapping("/{labId}/visit/{visitId}")
    public ResponseEntity<?> getVisit(
            @PathVariable Long labId,
            @PathVariable Long visitId
    ) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            return ResponseEntity.ok(visitService.getVisit(labId, visitId, currentUser));
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Auditable(module = "Lab")
    @GetMapping("/{labId}/patient/{patientId}/visit")
    public ResponseEntity<?> getVisitByPatient(
            @PathVariable Long labId,
            @PathVariable Long patientId
    ) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            return ResponseEntity.ok(visitService.getVisitByPatient(labId, patientId, currentUser));
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Auditable(module = "Lab")
    @GetMapping("/{labId}/visitsdatewise")
    public ResponseEntity<?> getVisitsByDateRange(
            @PathVariable Long labId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
            }

            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Lab is not accessible", HttpStatus.UNAUTHORIZED, null);
            }

            List<PatientDetailsDto> visits = List.of(); // Empty by default

            if (startDate != null && endDate != null) {
                visits = (List<PatientDetailsDto>) visitService.getVisitsByDateRange(labId, currentUser, startDate, endDate);
            }

            return ApiResponseHelper.successResponseWithDataAndMessage("Visits fetched successfully", HttpStatus.OK, visits);

        } catch (ResponseStatusException ex) {
            return ApiResponseHelper.errorResponseWithMessage(ex.getReason(), (HttpStatus) ex.getStatusCode());
        } catch (Exception e) {
            return ApiResponseHelper.errorResponseWithMessage(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Auditable(module = "Lab")
    @GetMapping("/{labId}/datewise-lab-visits")
    public ResponseEntity<?> getVisits(
            @PathVariable Long labId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
            }
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Lab is not accessible", HttpStatus.UNAUTHORIZED, null);
            }
            Object visits = visitService.getVisitDateWise(labId, startDate, endDate, currentUser);
//            List visits = (List) visitService.getVisitDateWise(labId, startDate, endDate, currentUser);
            return ApiResponseHelper.successResponseWithDataAndMessage("Visits fetched successfully", HttpStatus.OK, visits);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Auditable(module = "Lab")
    @GetMapping("/{labId}/datewise-patient-visits")
    public ResponseEntity<?> getPatientVisits(
            @PathVariable Long labId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
            }
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Lab is not accessible", HttpStatus.UNAUTHORIZED, null);
            }
            List<PatientVisitDTO> patientVisits = visitService.getPatientVisits(labId, startDate, endDate, currentUser);
            return ApiResponseHelper.successResponseWithDataAndMessage("Patient visits fetched successfully", HttpStatus.OK, patientVisits);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("No Visits Found", HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }


    @Auditable(module = "Lab")
    @DeleteMapping("/{labId}/delete-patient-visit/{visitId}")
    public ResponseEntity<?> deletePatientVisit(
            @PathVariable Long labId,
            @PathVariable Long visitId
    ) {
        try {
            // Authenticate user
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            
            // Check lab accessibility
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            
            // Delete the visit and all related data
            visitService.deleteVisit(labId, visitId, currentUser);
            return ApiResponseHelper.successResponse("Patient visit and all related data deleted successfully", HttpStatus.OK);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    //delete all visits of lab 
    @Auditable(module = "Lab")
    @DeleteMapping("/{labId}/delete-all-patient-visits")
    public ResponseEntity<?> deleteAllPatientVisits(
            @PathVariable Long labId
    ) {
        try {
            // Authenticate user
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }
            
            // Check lab accessibility
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            
            // Delete all visits and their related data for the lab
            int deletedCount = visitService.deleteAllVisitsForLab(labId, currentUser);
            return ApiResponseHelper.successResponse("Successfully deleted " + deletedCount + " visits and all related data", HttpStatus.OK);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
