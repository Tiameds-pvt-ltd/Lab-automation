package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.InsuranceDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.InsuranceServices;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;

import java.util.Optional;

@Transactional
@RestController
@RequestMapping("/lab/admin/insurance")
@Tag(name = "Insurance", description = "Endpoints for managing insurance admin can add insurance to a lab")
public class InsuranceController {

    private final InsuranceServices insuranceServices;
    private final UserService userService;
    private final LabRepository labRepository;
    private final LabAccessableFilter labAccessableFilter;


    public InsuranceController(InsuranceServices insuranceServices, UserService userService, LabRepository labRepository, LabAccessableFilter labAccessableFilter) {
        this.insuranceServices = insuranceServices;
        this.userService = userService;
        this.labRepository = labRepository;
        this.labAccessableFilter = labAccessableFilter;
    }


    // Add insurance
    @PostMapping("{labId}")
    public ResponseEntity<?> addInsurance(
            @PathVariable("labId") Long labId,
            @RequestBody InsuranceDTO insuranceDTO) {

        try {
            // Authenticate the user using the provided token
            User currentUser = getAuthenticatedUser().orElse(null);
            if (currentUser == null) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check if the lab exists in the repository
            Lab lab = labRepository.findById(labId).orElseThrow(() -> new RuntimeException("Lab not found"));

            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Verify if the current user is associated with the lab
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }
            // Delegate to the service layer
            insuranceServices.addInsurance(labId, insuranceDTO);

            return ApiResponseHelper.successResponse("Insurance added successfully", insuranceDTO);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // get all insurance of a particular lab where labid and insuranceid are matched
    @GetMapping("{labId}")
    public ResponseEntity<?> getAllInsurance(
            @PathVariable("labId") Long labId) {
        try {
            // Authenticate the user using the provided token
            User currentUser = getAuthenticatedUser().orElse(null);
            if (currentUser == null) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check if the lab exists in the repository
            Lab lab = labRepository.findById(labId).orElseThrow(() -> new RuntimeException("Lab not found"));

            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Verify if the current user is associated with the lab
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }

            // Delegate to the service layer
            return ApiResponseHelper.successResponse("Insurance retrieved successfully", insuranceServices.getAllInsurance(labId));

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // get insurance by id where labid and insuranceid are matched means insurance is associated with that lab only
    @GetMapping("{labId}/insurance/{insuranceId}")
    public ResponseEntity<?> getInsuranceById(
            @PathVariable("labId") Long labId,
            @PathVariable("insuranceId") Long insuranceId) {
        try {
            // Authenticate the user using the provided token
            User currentUser = getAuthenticatedUser().orElse(null);
            if (currentUser == null) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check if the lab exists in the repository
            Lab lab = labRepository.findById(labId).orElseThrow(() -> new RuntimeException("Lab not found"));

            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Verify if the current user is associated with the lab
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }

            // Delegate to the service layer
            return ApiResponseHelper.successResponse("Insurance retrieved successfully", insuranceServices.getInsuranceById(labId, insuranceId));

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    // update insurance by id where labid and insuranceid are matched means insurance is associated with that lab only
    @PutMapping("{labId}/insurance/{insuranceId}")
    public ResponseEntity<?> updateInsurance(
            @PathVariable("labId") Long labId,
            @PathVariable("insuranceId") Long insuranceId,
            @RequestBody InsuranceDTO insuranceDTO) {
        try {
            // Authenticate the user using the provided token
            User currentUser = getAuthenticatedUser().orElse(null);
            if (currentUser == null) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check if the lab exists in the repository
            Lab lab = labRepository.findById(labId).orElseThrow(() -> new RuntimeException("Lab not found"));

            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Verify if the current user is associated with the lab
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }

            // Delegate to the service layer
            insuranceServices.updateInsurance(labId, insuranceId, insuranceDTO);

            return ApiResponseHelper.successResponse("Insurance updated successfully", insuranceDTO);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    // delete insurance by id where labid and insuranceid are matched means insurance is associated with that lab only
    @DeleteMapping("{labId}/insurance/{insuranceId}")
    public ResponseEntity<?> deleteInsurance(
            @PathVariable("labId") Long labId,
            @PathVariable("insuranceId") Long insuranceId) {

        try {
            // Authenticate the user using the provided token
            User currentUser = getAuthenticatedUser().orElse(null);
            if (currentUser == null) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            // Check if the lab exists in the repository
            Lab lab = labRepository.findById(labId).orElseThrow(() -> new RuntimeException("Lab not found"));

            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Verify if the current user is associated with the lab
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }

            // Delegate to the service layer
            insuranceServices.deleteInsurance(labId, insuranceId);

            return ApiResponseHelper.successResponse("Insurance deleted successfully", null);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
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
