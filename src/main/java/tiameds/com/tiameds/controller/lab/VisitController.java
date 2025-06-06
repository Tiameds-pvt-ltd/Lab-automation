package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tiameds.com.tiameds.dto.lab.PatientDTO;
import tiameds.com.tiameds.dto.lab.PatientDetailsDto;
import tiameds.com.tiameds.dto.lab.VisitDTO;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.lab.BillingService;
import tiameds.com.tiameds.services.lab.VisitService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Transactional
@RestController
@RequestMapping("/lab")
@Tag(name = "Visit Controller", description = "mannage the patient visit in the repective controller")
public class VisitController {
    private final VisitService visitService;
    private final UserAuthService userAuthService;
    private final LabAccessableFilter labAccessableFilter;

    public VisitController(VisitService visitService, BillingService billingService, UserAuthService userAuthService, LabAccessableFilter labAccessableFilter) {
        this.visitService = visitService;
        this.userAuthService = userAuthService;
        this.labAccessableFilter = labAccessableFilter;
    }
    @PostMapping("/{labId}/add-visit/{patientId}")
    public ResponseEntity<?> addVisit(
            @PathVariable Long labId,
            @PathVariable Long patientId,
            @RequestBody VisitDTO visitDTO,
            @RequestHeader("Authorization") String token
    ) {
        try {
            // Validate token format
            Optional<User> currentUser = userAuthService.authenticateUser(token);
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

    @GetMapping("/{labId}/visits")
    public ResponseEntity<?> getVisits(
            @PathVariable Long labId,
            @RequestHeader("Authorization") String token
    ) {
        try {
            Optional<User> currentUser = userAuthService.authenticateUser(token);
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

    @PutMapping("/{labId}/update-visit/{visitId}")
    public ResponseEntity<?> updateVisit(
            @PathVariable Long labId,
            @PathVariable Long visitId,
            @RequestBody VisitDTO visitDTO,
            @RequestHeader("Authorization") String token
    ) {
        try {
            Optional<User> currentUser = userAuthService.authenticateUser(token);
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

    @DeleteMapping("/{labId}/delete-visit/{visitId}")
    public ResponseEntity<?> deleteVisit(
            @PathVariable Long labId,
            @PathVariable Long visitId,
            @RequestHeader("Authorization") String token
    ) {
        try {
            Optional<User> currentUser = userAuthService.authenticateUser(token);
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

    @GetMapping("/{labId}/visit/{visitId}")
    public ResponseEntity<?> getVisit(
            @PathVariable Long labId,
            @PathVariable Long visitId,
            @RequestHeader("Authorization") String token
    ) {
        try {
            Optional<User> currentUser = userAuthService.authenticateUser(token);
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

    @GetMapping("/{labId}/patient/{patientId}/visit")
    public ResponseEntity<?> getVisitByPatient(
            @PathVariable Long labId,
            @PathVariable Long patientId,
            @RequestHeader("Authorization") String token
    ) {
        try {
            Optional<User> currentUser = userAuthService.authenticateUser(token);
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


    @GetMapping("/{labId}/visitsdatewise")
    public ResponseEntity<?> getVisitsByDateRange(
            @PathVariable Long labId,
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            Optional<User> currentUser = userAuthService.authenticateUser(token);
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


}
