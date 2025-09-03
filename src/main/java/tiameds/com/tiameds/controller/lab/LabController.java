package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.dto.lab.LabListDTO;
import tiameds.com.tiameds.dto.lab.LabRequestDTO;
import tiameds.com.tiameds.dto.lab.LabResponseDTO;
import tiameds.com.tiameds.dto.lab.UserResponseDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.services.lab.TestReferenceServices;
import tiameds.com.tiameds.services.lab.TestServices;
import tiameds.com.tiameds.services.lab.UserLabService;
import tiameds.com.tiameds.utils.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;


@Transactional
@RestController
@RequestMapping("/lab/admin")
@Tag(name = "Lab Admin", description = "Endpoints for Lab Admin where lab admin can manage labs, add members, and handle lab-related operations")
public class LabController {
    private final UserLabService userService;
    private final LabRepository labRepository;
    private final UserAuthService userAuthService;
    private final LabAccessableFilter labAccessableFilter;
    private UserLabService userLabService;
    private final TestServices testServices;
    private final TestReferenceServices testReferenceServices;
    private static final Logger LOGGER = Logger.getLogger(LabController.class.getName());

    public LabController(UserLabService userService, LabRepository labRepository, UserAuthService userAuthService, LabAccessableFilter labAccessableFilter, UserLabService userLabService, TestServices testServices, TestReferenceServices testReferenceServices) {
        this.userService = userService;
        this.labRepository = labRepository;
        this.userAuthService = userAuthService;
        this.labAccessableFilter = labAccessableFilter;
        this.userLabService = userLabService;
        this.testServices = testServices;
        this.testReferenceServices = testReferenceServices;
    }

    // ---------- Get all labs created by the user ----------
    @GetMapping("/get-labs")
    public ResponseEntity<?> getLabsCreatedByUser(
            @RequestHeader("Authorization") String token) {
        Optional<User> currentUserOptional = userAuthService.authenticateUser(token);
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
            @PathVariable Long labId,
            @RequestHeader("Authorization") String token) {
        Optional<User> currentUserOptional = userAuthService.authenticateUser(token);
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
            @RequestBody LabRequestDTO labRequestDTO,
            @RequestHeader("Authorization") String token) {
        Optional<User> currentUserOptional = userAuthService.authenticateUser(token);
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
    public ResponseEntity<Map<String, Object>> addLab(
            @RequestBody LabRequestDTO labRequestDTO,
            @RequestHeader("Authorization") String token) {

        Optional<User> currentUserOptional = userAuthService.authenticateUser(token);
        if (currentUserOptional.isEmpty()) {
            ApiResponse<String> response = new ApiResponse<>("error", "User not found", null);
            return new ResponseEntity(response, HttpStatus.UNAUTHORIZED);
        }
        User currentUser = currentUserOptional.get();

        // Check if the lab already exists
        if (userService.existsLabByName(labRequestDTO.getName())) {
            ApiResponse<String> response = new ApiResponse<>("error", "Lab already exists", null);
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
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
            addMemberToLab(savedLab.getId(), currentUser.getId(), token);
        } catch (Exception e) {
            ApiResponse<String> response = new ApiResponse<>("error", "Failed to add user as member", null);
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
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

            ClassPathResource referencePointResource = new ClassPathResource("Lab_Modified.xlsx-Sheet1.csv");
            try (BufferedReader referencePointReader = new BufferedReader(new InputStreamReader(referencePointResource.getInputStream()))) {
                // Convert BufferedReader content to MultipartFile for the service
                StringBuilder contentBuilder = new StringBuilder();
                String line;
                while ((line = referencePointReader.readLine()) != null) {
                    contentBuilder.append(line).append("\n");
                }
                byte[] contentBytes = contentBuilder.toString().getBytes();
                MultipartFile referencePointFile = new CustomMockMultipartFile(
                        "Lab_Modified.xlsx-Sheet1.csv",
                        "Lab_Modified.xlsx-Sheet1.csv",
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

    private void addMemberToLab(Long labId, Long userId, String token) {
        User currentUser = userAuthService.authenticateUser(token).orElse(null);
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
}

