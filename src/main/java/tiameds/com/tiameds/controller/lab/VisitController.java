package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.VisitDTO;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.lab.BillingService;
import tiameds.com.tiameds.services.lab.VisitService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;

import java.util.Optional;


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


    // Add visit for an existing patient at the lab
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

            // Create the visit (save to DB)
            visitService.addVisit(labId, patientId, visitDTO, currentUser);

            return ApiResponseHelper.successResponse("Visit added successfully", HttpStatus.OK);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // get list of patient visits of respective lab
    @GetMapping("/{labId}/visits")
    public ResponseEntity<?> getVisits(
            @PathVariable Long labId,
            @RequestHeader("Authorization") String token
    ) {
        try {
            // Validate token and authenticate user
            Optional<User> currentUser = userAuthService.authenticateUser(token);
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
            }

            // Check lab accessibility
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Lab is not accessible", HttpStatus.UNAUTHORIZED, null);
            }

            // Fetch the list of visits
            Object visits = visitService.getVisits(labId, currentUser);

            // Return success response with the list of visits
            return ApiResponseHelper.successResponseWithDataAndMessage("Visits fetched successfully", HttpStatus.OK, visits);

        } catch (Exception e) {
            // Handle unexpected errors
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // update the visit details
    @PutMapping("/{labId}/update-visit/{visitId}")
    public ResponseEntity<?> updateVisit(
            @PathVariable Long labId,
            @PathVariable Long visitId,
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

            // Update the visit
            visitService.updateVisit(labId, visitId, visitDTO, currentUser);

            return ApiResponseHelper.successResponse("Visit updated successfully", HttpStatus.OK);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // delete the visit
    @DeleteMapping("/{labId}/delete-visit/{visitId}")
    public ResponseEntity<?> deleteVisit(
            @PathVariable Long labId,
            @PathVariable Long visitId,
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

            // Delete the visit
            visitService.deleteVisit(labId, visitId, currentUser);

            return ApiResponseHelper.successResponse("Visit deleted successfully", HttpStatus.OK);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // get the visit details by visit ID
    @GetMapping("/{labId}/visit/{visitId}")
    public ResponseEntity<?> getVisit(
            @PathVariable Long labId,
            @PathVariable Long visitId,
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

            // Get the visit details
            return ResponseEntity.ok(visitService.getVisit(labId, visitId, currentUser));

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // get the visit details by patient ID
    @GetMapping("/{labId}/patient/{patientId}/visit")
    public ResponseEntity<?> getVisitByPatient(
            @PathVariable Long labId,
            @PathVariable Long patientId,
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

            // Get the visit details
            return ResponseEntity.ok(visitService.getVisitByPatient(labId, patientId, currentUser));

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
