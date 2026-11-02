package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.dto.lab.LabListFullDTO;
import tiameds.com.tiameds.dto.lab.LabMemberDTO;
import tiameds.com.tiameds.dto.lab.LabRequestDTO;
import tiameds.com.tiameds.dto.lab.LabLogoUploadRequestDTO;
import tiameds.com.tiameds.dto.lab.LabLogoUploadResponseDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.dto.lab.LabCreationResponseDTO;
import tiameds.com.tiameds.services.lab.LabCreationService;
import tiameds.com.tiameds.services.onboarding.TempLabService;
import tiameds.com.tiameds.services.lab.UserLabService;
import tiameds.com.tiameds.services.lab.LabService;
import tiameds.com.tiameds.utils.ApiResponse;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@Slf4j
@RestController
@RequestMapping("/lab/admin")
@Tag(name = "Lab Admin", description = "Endpoints for Lab Admin where lab admin can manage labs, add members, and handle lab-related operations")
public class LabController {
    private final UserLabService userLabService;
    private final LabRepository labRepository;
    private final LabAccessableFilter labAccessableFilter;
    private final TempLabService tempLabService;
    private final UserService userService;
    private final LabCreationService labCreationService;
    private final LabService labService;

    public LabController(UserLabService userLabService,
                         LabRepository labRepository,
                         LabAccessableFilter labAccessableFilter,
                         TempLabService tempLabService,
                         UserService userService,
                         LabCreationService labCreationService,
                         LabService labService) {
        this.userLabService = userLabService;
        this.labRepository = labRepository;
        this.labAccessableFilter = labAccessableFilter;
        this.tempLabService = tempLabService;
        this.userService = userService;
        this.labCreationService = labCreationService;
        this.labService = labService;
    }

    // ---------- Get all labs created by the user ----------


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


    // ---------- Request a new lab: it is created only after a platform admin approves it ----------
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
            // Stores the request as PENDING; the lab, its membership and the default data are created on approval
            LabCreationResponseDTO response = labCreationService.submitLabForApproval(labRequestDTO, currentUser);
            log.info("Lab request submitted for approval by user {} (Lab: {})",
                    response.getUsername(), response.getLabName());

            return ApiResponseHelper.successResponseWithDataAndMessage(
                    response.getMessage(),
                    HttpStatus.ACCEPTED,
                    response);

        } catch (IllegalArgumentException e) {
            log.warn("Lab request failed due to validation error: {}", e.getMessage());
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST,
                    null);
        } catch (Exception e) {
            log.error("Unexpected error during lab request for user {}: {}",
                    currentUser.getUsername(), e.getMessage(), e);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "An error occurred while submitting the lab request. Please try again or contact support.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    null);
        }
    }

    // ---------- Status of the lab requests of the current user (pending / approved / rejected with reason) ----------
    @GetMapping("/lab-requests")
    public ResponseEntity<Map<String, Object>> getMyLabRequests() {
        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
        }
        return ApiResponseHelper.successResponseWithDataAndMessage(
                "Lab requests fetched successfully",
                HttpStatus.OK,
                tempLabService.getRequestsOfUser(currentUserOptional.get().getId()));
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


    @Transactional
    @GetMapping("/get-labs")
    public ResponseEntity<?> getLabsCreatedByUser() {
        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
        }
        User currentUser = currentUserOptional.get();
        List<LabListFullDTO> labListDTOs = labService.getLabsCreatedByUser(currentUser);
        if (labListDTOs.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("No labs found", HttpStatus.OK, null);
        }
        return ApiResponseHelper.successResponseWithDataAndMessage("Labs fetched successfully", HttpStatus.OK, labListDTOs);
    }

    @PostMapping("/lab-logo/upload-url")
    public ResponseEntity<?> getLabLogoUploadUrl(@RequestBody LabLogoUploadRequestDTO requestDTO) {
        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
        }
        try {
            LabLogoUploadResponseDTO response = labService.createLabLogoUploadUrl(currentUserOptional.get(), requestDTO);
            return ApiResponseHelper.successResponseWithDataAndMessage("Upload URL generated successfully", HttpStatus.OK, response);
        } catch (IllegalArgumentException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.BAD_REQUEST, null);
        } catch (IllegalStateException e) {
            HttpStatus status = e.getMessage() != null && e.getMessage().toLowerCase().contains("configured")
                    ? HttpStatus.INTERNAL_SERVER_ERROR
                    : HttpStatus.UNAUTHORIZED;
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), status, null);
        }
    }


    //--------------------- Get lab by ID --------------------
    @GetMapping("/get-lab-by-id/{labId}")
    public ResponseEntity<?> getLabById(@PathVariable Long labId) {
        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
        }
        User currentUser = currentUserOptional.get();
        Optional<Lab> labOptional = labRepository.findLabWithMembers(labId);
        if (labOptional.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("Lab not found", HttpStatus.NOT_FOUND, null);
        }
        Lab lab = labOptional.get();
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (!isAccessible) {
            return ApiResponseHelper.successResponseWithDataAndMessage("Lab not accessible", HttpStatus.UNAUTHORIZED, null);
        }
        if (!lab.getMembers().contains(currentUser)) {
            return ApiResponseHelper.successResponseWithDataAndMessage("User is not a member of this lab", HttpStatus.UNAUTHORIZED, null);
        }
        LabListFullDTO labDetails = labService.toLabListFullDTO(lab);
        return ApiResponseHelper.successResponseWithDataAndMessage("Lab fetched successfully", HttpStatus.OK, labDetails);
    }

    //----------------------- update lab by id --------------------
    @PutMapping("/update-lab-by-id/{labId}")
    public ResponseEntity<?> updateLabById(@PathVariable Long labId, @RequestBody LabRequestDTO labRequestDTO) {
        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
        }
        User currentUser = currentUserOptional.get();
        try {
            LabService.UpdateLabResult result = labService.updateLabById(currentUser, labId, labRequestDTO);
            return ApiResponseHelper.successResponseWithDataAndMessage(result.getMessage(), result.getStatus(), result.getData());
        } catch (IllegalArgumentException e) {
            // Validation error in the onboarding fields: the whole update was rolled back
            log.warn("Lab update failed for lab {}: {}", labId, e.getMessage());
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.BAD_REQUEST, null);
        }
    }

    //  get all members of a lab
    @GetMapping("/get-all-members-of-a-lab/{labId}")
    public ResponseEntity<?> getAllMembersOfALab(@PathVariable Long labId) {
        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("User not found", HttpStatus.UNAUTHORIZED, null);
        }
        List<LabMemberDTO> members = labService.getAllMembersOfALab(labId);
        if (members.isEmpty()) {
            return ApiResponseHelper.successResponseWithDataAndMessage("No members found", HttpStatus.OK, null);
        }
        return ApiResponseHelper.successResponseWithDataAndMessage("Members fetched successfully", HttpStatus.OK, members);
    }
}