package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.BillingDTO;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.lab.BillingService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;

import java.util.List;
import java.util.Optional;

@Transactional
@RestController
@RequestMapping("/lab")
@Tag(name = "Billing Controller", description = "Manage billing in the lab")
public class BillingController {


    private final BillingService billingService;
    private final UserAuthService userAuthService;
    private final LabAccessableFilter labAccessableFilter;

    public BillingController(BillingService billingService,
                             UserAuthService userAuthService, LabAccessableFilter labAccessableFilter) {
        this.billingService = billingService;
        this.userAuthService = userAuthService;
        this.labAccessableFilter = labAccessableFilter;
    }

    // Get all billings of a respective lab
    @GetMapping("/{labId}/billing")
    public ResponseEntity<?> getBillingList(
            @RequestHeader("Authorization") String token,
            BillingDTO billingDTO,
            @PathVariable("labId") Long labId) {

        try {
            // Authenticate user
            Optional<User> currentUser = userAuthService.authenticateUser(token);
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check lab accessibility
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Fetch billing list
            List<BillingDTO> billingList = billingService.getBillingList(labId, currentUser, billingDTO);
            return ApiResponseHelper.successResponse("Billing list fetched successfully", billingList);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // get billing details by by patient id of a respective lab
    @GetMapping("/{labId}/billing/{patientId}")
    public ResponseEntity<?> getBillingDetailsByPatientId(
            @RequestHeader("Authorization") String token,
            @PathVariable("labId") Long labId,
            @PathVariable("patientId") Long patientId) {

        try {
            // Authenticate user
            Optional<User> currentUser = userAuthService.authenticateUser(token);
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check lab accessibility
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Fetch billing details
            List<BillingDTO> billingDetails = billingService.getBillingDetailsByPatientId(labId, currentUser, patientId);
            return ApiResponseHelper.successResponse("Billing details fetched successfully", billingDetails);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
