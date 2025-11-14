package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.dto.lab.LabListDTO;
import tiameds.com.tiameds.dto.lab.LabRequestDTO;
import tiameds.com.tiameds.dto.lab.LabResponseDTO;
import tiameds.com.tiameds.dto.lab.UserResponseDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.TestReferenceServices;
import tiameds.com.tiameds.services.lab.TestServices;
import tiameds.com.tiameds.services.lab.UserLabService;
import tiameds.com.tiameds.utils.ApiResponse;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.CustomMockMultipartFile;
import tiameds.com.tiameds.utils.LabAccessableFilter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;


@Transactional
@RestController
@RequestMapping("/lab/admin")
@Tag(name = "Lab Admin", description = "Endpoints for Lab Admin where lab admin can manage labs, add members, and handle lab-related operations")
public class LabController {
    private final UserLabService userLabService;
    private final LabRepository labRepository;
    private final LabAccessableFilter labAccessableFilter;
    private final TestServices testServices;
    private final TestReferenceServices testReferenceServices;
    private final UserService userService;

    public LabController(UserLabService userLabService,
                         LabRepository labRepository,
                         LabAccessableFilter labAccessableFilter,
                         TestServices testServices,
                         TestReferenceServices testReferenceServices,
                         UserService userService) {
        this.userLabService = userLabService;
        this.labRepository = labRepository;
        this.labAccessableFilter = labAccessableFilter;
        this.testServices = testServices;
        this.testReferenceServices = testReferenceServices;
        this.userService = userService;
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


    @Transactional
    @PostMapping("/add-lab")
    public ResponseEntity<?> addLab(
            @RequestBody LabRequestDTO labRequestDTO) {

        Optional<User> currentUserOptional = getAuthenticatedUser();
        if (currentUserOptional.isEmpty()) {
            ApiResponse<String> response = new ApiResponse<>("error", "User not found", null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        User currentUser = currentUserOptional.get();

        // Check if the lab already exists
        if (userLabService.existsLabByName(labRequestDTO.getName())) {
            ApiResponse<String> response = new ApiResponse<>("error", "Lab already exists", null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        Lab lab = new Lab();
        lab.setName(labRequestDTO.getName());
        lab.setAddress(labRequestDTO.getAddress());
        lab.setCity(labRequestDTO.getCity());
        lab.setState(labRequestDTO.getState());
        lab.setDescription(labRequestDTO.getDescription());
        lab.setIsActive(true);
        lab.setCreatedBy(currentUser);

        // Set new fields
        lab.setLabLogo(labRequestDTO.getLabLogo());
        lab.setLicenseNumber(labRequestDTO.getLicenseNumber());
        lab.setLabType(labRequestDTO.getLabType());
        lab.setLabZip(labRequestDTO.getLabZip());
        lab.setLabCountry(labRequestDTO.getLabCountry());
        lab.setLabPhone(labRequestDTO.getLabPhone());
        lab.setLabEmail(labRequestDTO.getLabEmail());
        lab.setDirectorName(labRequestDTO.getDirectorName());
        lab.setDirectorEmail(labRequestDTO.getDirectorEmail());
        lab.setDirectorPhone(labRequestDTO.getDirectorPhone());
        lab.setCertificationBody(labRequestDTO.getCertificationBody());
        lab.setLabCertificate(labRequestDTO.getLabCertificate());
        lab.setDirectorGovtId(labRequestDTO.getDirectorGovtId());
        lab.setLabBusinessRegistration(labRequestDTO.getLabBusinessRegistration());
        lab.setLabLicense(labRequestDTO.getLabLicense());
        lab.setTaxId(labRequestDTO.getTaxId());
        lab.setLabAccreditation(labRequestDTO.getLabAccreditation());
        lab.setDataPrivacyAgreement(labRequestDTO.getDataPrivacyAgreement());
        Lab savedLab = labRepository.save(lab);
        try {
            addMemberToLab(savedLab.getId(), currentUser.getId());
        } catch (Exception e) {
            ApiResponse<String> response = new ApiResponse<>("error", "Failed to add user as member", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        // Return success response
        try {
            // Process price list file
            ClassPathResource priceListResource = new ClassPathResource("tiamed_price_list.csv");
            try (BufferedReader priceListReader = new BufferedReader(new InputStreamReader(((ClassPathResource) priceListResource).getInputStream()))) {
                // Convert BufferedReader content to MultipartFile for the service
                StringBuilder contentBuilder = new StringBuilder();
                String line;
                while ((line = priceListReader.readLine()) != null) {
                    contentBuilder.append(line).append("\n");
                }
                byte[] contentBytes = contentBuilder.toString().getBytes();
                MultipartFile priceListFile = new CustomMockMultipartFile(
                        "tiamed_price_list.csv",
                        "tiamed_price_list.csv",
                        "text/csv",
                        contentBytes
                );
                testServices.uploadCSV(priceListFile, savedLab);
            }

            ClassPathResource referencePointResource = new ClassPathResource("sample_test_references_with_reference_ranges.csv");
            try (BufferedReader referencePointReader = new BufferedReader(new InputStreamReader(referencePointResource.getInputStream()))) {
                // Convert BufferedReader content to MultipartFile for the service
                StringBuilder contentBuilder = new StringBuilder();
                String line;
                while ((line = referencePointReader.readLine()) != null) {
                    contentBuilder.append(line).append("\n");
                }
                byte[] contentBytes = contentBuilder.toString().getBytes();
                MultipartFile referencePointFile = new CustomMockMultipartFile(
                        "sample_test_references_with_reference_ranges.csv",
                        "sample_test_references_with_reference_ranges.csv",
                        "text/csv",
                        contentBytes
                );
                testReferenceServices.uploadCsv(savedLab, referencePointFile, currentUser);
            }
        } catch (Exception e) {
            // Log the error but don't fail the entire operation
            System.err.println("Error processing default CSV files: " + e.getMessage());
        }
        UserResponseDTO userResponseDTO = new UserResponseDTO(    // Create UserResponseDTO
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getEmail(),
                currentUser.getFirstName(),
                currentUser.getLastName()
        );
        LabResponseDTO labResponseDTO = new LabResponseDTO(
                savedLab.getId(),
                savedLab.getName(),
                savedLab.getAddress(),
                savedLab.getCity(),
                savedLab.getState(),
                savedLab.getDescription(),
                userResponseDTO
        );
        return ApiResponseHelper.successResponseWithDataAndMessage("Lab created successfully and user added as a member", HttpStatus.OK, labResponseDTO);
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