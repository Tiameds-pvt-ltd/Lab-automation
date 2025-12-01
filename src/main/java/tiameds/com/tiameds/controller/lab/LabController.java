package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.LabListDTO;
import tiameds.com.tiameds.dto.lab.LabRequestDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.dto.lab.LabCreationResponseDTO;
import tiameds.com.tiameds.services.lab.LabCreationService;
import tiameds.com.tiameds.services.lab.LabDefaultDataService;
import tiameds.com.tiameds.services.lab.UserLabService;
import tiameds.com.tiameds.utils.ApiResponse;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Transactional
@RestController
@RequestMapping("/lab/admin")
@Tag(name = "Lab Admin", description = "Endpoints for Lab Admin where lab admin can manage labs, add members, and handle lab-related operations")
public class LabController {
    private final UserLabService userLabService;
    private final LabRepository labRepository;
    private final LabAccessableFilter labAccessableFilter;
    private final LabDefaultDataService labDefaultDataService;
    private final UserService userService;
    private final LabCreationService labCreationService;

    public LabController(UserLabService userLabService,
                         LabRepository labRepository,
                         LabAccessableFilter labAccessableFilter,
                         LabDefaultDataService labDefaultDataService,
                         UserService userService,
                         LabCreationService labCreationService) {
        this.userLabService = userLabService;
        this.labRepository = labRepository;
        this.labAccessableFilter = labAccessableFilter;
        this.labDefaultDataService = labDefaultDataService;
        this.userService = userService;
        this.labCreationService = labCreationService;
    }

    // ---------- Get all labs created by the user ----------
    @GetMapping("/get-labs")
    public ResponseEntity<?> getLabsCreatedByUser() {
        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
        }
        User currentUser = currentUserOptional.get();
        List<Lab> labs = labRepository.findByCreatedBy(currentUser);
        if (labs.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("No labs found", HttpStatus.OK, null);
        }
        List<LabListDTO> labListDTOs = labs.stream()
                .map(lab -> new LabListDTO(
                        lab.getId(),
                        lab.getName(),
                        lab.getAddress(),
                        lab.getCity(),
                        lab.getState(),
                        lab.getIsActive(),
                        lab.getDescription(),
                        lab.getCreatedBy().getUsername()
                ))
                .toList();
        return ApiResponseHelper.successResponseWithDataAndMessage("Labs fetched successfully", HttpStatus.OK, labListDTOs);
    }


    @DeleteMapping("/delete-lab/{labId}")
    public ResponseEntity<?> deleteLab(
            @PathVariable Long labId) {
        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            ApiResponse<String> errorResponse = new ApiResponse<>("error", "User not found", null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        User currentUser = currentUserOptional.get();
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            ApiResponse<String> errorResponse = new ApiResponse<>("error", "Lab not found", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        Lab lab = labOptional.get();
        if (!lab.getCreatedBy().equals(currentUser)) {
            ApiResponse<String> errorResponse = new ApiResponse<>("error", "You are not authorized to delete this lab", null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        labRepository.delete(lab);
        return ApiResponseHelper.successResponseWithDataAndMessage("Lab deleted successfully", HttpStatus.OK, null);
    }

    //--------------------- Update lab by ID --------------------
    @PutMapping("/update-lab/{labId}")
    public ResponseEntity<?> updateLab(
            @PathVariable Long labId,
            @RequestBody LabRequestDTO labRequestDTO) {
        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            ApiResponse<String> errorResponse = new ApiResponse<>("error", "User not found", null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        User currentUser = currentUserOptional.get();
        Optional<Lab> labOptional = labRepository.findById(labId);
        if (labOptional.isEmpty()) {
            ApiResponse<String> errorResponse = new ApiResponse<>("error", "Lab not found", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        Lab lab = labOptional.get();
        if (!lab.getCreatedBy().equals(currentUser)) {
            ApiResponse<String> errorResponse = new ApiResponse<>("error", "You are not authorized to update this lab", null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        lab.setName(labRequestDTO.getName());
        lab.setAddress(labRequestDTO.getAddress());
        lab.setCity(labRequestDTO.getCity());
        lab.setState(labRequestDTO.getState());
        lab.setDescription(labRequestDTO.getDescription());
        labRepository.save(lab);
        return ApiResponseHelper.successResponseWithDataAndMessage("Lab updated successfully", HttpStatus.OK, lab);
    }




    // ---------- Create a new lab and add the current user as a member ----------
    @Transactional
    @PostMapping("/add-lab")
    public ResponseEntity<Map<String, Object>> addLab(
            @RequestBody LabRequestDTO labRequestDTO) {

        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "User not found",
                    HttpStatus.UNAUTHORIZED,
                    null);
        }
        User currentUser = currentUserOptional.get();

        try {
            // Create lab and associate user in a single transaction
            LabCreationResponseDTO response = labCreationService.createLabWithUserAssociation(labRequestDTO, currentUser);
            
            // Safe logging with null checks
            String username = response != null ? response.getUsername() : "unknown";
            String labName = response != null ? response.getLabName() : "unknown";
            log.info("Lab created successfully for user: {} (Lab: {})", username, labName);

            // Safe message extraction with fallback
            String message = (response != null && response.getMessage() != null) 
                    ? response.getMessage() 
                    : "Lab created successfully and user added as a member";

            // Return response immediately - don't wait for default data upload
            ResponseEntity<Map<String, Object>> successResponse = ApiResponseHelper.successResponseWithDataAndMessage(
                    message,
                    HttpStatus.CREATED,
                    response);
            
            // Trigger default data upload asynchronously - never block the response
            triggerDefaultDataUploadAsync(response);
            
            return successResponse;

        } catch (IllegalArgumentException e) {
            log.warn("Lab creation failed due to validation error: {}", e.getMessage());
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST,
                    null);
        } catch (Exception e) {
            log.error("Unexpected error during lab creation for user {}: {}", 
                    currentUser.getUsername(), e.getMessage(), e);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "An error occurred during lab creation. Please try again or contact support.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    null);
        }
    }
    /**
     * Triggers default data upload asynchronously without blocking the HTTP response.
     * This ensures lab creation response is returned immediately even if upload takes time.
     */
    private void triggerDefaultDataUploadAsync(LabCreationResponseDTO response) {
        if (response == null) {
            return;
        }
        Long labId = response.getLabId();
        Long userId = response.getUserId();
        
        if (labId == null || userId == null) {
            log.warn("Skipping default data upload due to null labId or userId (labId={}, userId={})", labId, userId);
            return;
        }
        // Execute asynchronously - don't block the HTTP response
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting async default data upload for lab {} (userId: {})", labId, userId);
                labDefaultDataService.uploadDefaultData(labId, userId);
                log.info("Completed async default data upload for lab {}", labId);
            } catch (Exception e) {
                // Log but never throw - lab creation succeeded, this is just a convenience feature
                log.error("Default data upload failed but lab creation succeeded for lab {}: {}", 
                        labId, e.getMessage(), e);
            }
        });
    }

    private void addMemberToLab(Long labId, Long userId) {
        User currentUser = getAuthenticatedUser().orElse(null);
        if (currentUser == null) {
            throw new IllegalStateException("User not found or unauthorized");
        }
        Lab lab = labRepository.findById(labId).orElse(null);
        if (lab == null) {
            throw new IllegalStateException("Lab not found");
        }
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (!isAccessible) {
            throw new IllegalStateException("Lab is not accessible");
        }
        User userToAdd = userLabService.getUserById(userId);
        if (userToAdd == null) {
            throw new IllegalStateException("User to be added not found");
        }
        if (!lab.getCreatedBy().equals(currentUser)) {
            throw new IllegalStateException("You are not authorized to add members to this lab");
        }
        if (lab.getMembers().contains(userToAdd)) {
            throw new IllegalStateException("User is already a member of this lab");
        }
        lab.getMembers().add(userToAdd);
        labRepository.save(lab);
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