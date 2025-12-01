package tiameds.com.tiameds.controller.lab;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.dto.lab.TestReferenceDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.TestReferenceEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.services.lab.TestReferenceServices;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping("/lab/test-reference")
@Tag(name = "Test Reference Controller", description = "APIs for test reference")
public class TestReferenceController {

    private final LabRepository labRepository;
    private final TestReferenceServices testReferenceServices;
    private final LabAccessableFilter labAccessableFilter;
    private final UserService userService;
    private static final Logger LOGGER = Logger.getLogger(TestReferenceController.class.getName());

    public TestReferenceController(
            LabRepository labRepository,
            TestReferenceServices testReferenceServices,
            LabAccessableFilter labAccessableFilter,
            UserService userService) {
        this.labRepository = labRepository;
        this.testReferenceServices = testReferenceServices;
        this.labAccessableFilter = labAccessableFilter;
        this.userService = userService;
    }

    @Transactional
    @PostMapping("/{labId}/csv/upload")
    public ResponseEntity<?> uploadCsv(
            @PathVariable Long labId,
            @RequestParam("file") MultipartFile file) {
        try {
            Optional<User> userOptional = getAuthenticatedUser();
            if (userOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
            }
            User currentUser = userOptional.get();
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }
            Lab lab = labOptional.get();
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not authorized for this lab", HttpStatus.UNAUTHORIZED);
            }
            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }
            if (file.isEmpty() || !"text/csv".equals(file.getContentType())) {
                return ApiResponseHelper.errorResponse("Please upload a valid CSV file", HttpStatus.BAD_REQUEST);
            }
            List<TestReferenceEntity> testReferenceEntities = testReferenceServices.uploadCsv(lab, file, currentUser);
            return ApiResponseHelper.successResponseWithDataAndMessage("Test references uploaded successfully", HttpStatus.CREATED, testReferenceEntities);
        } catch (Exception e) {
            LOGGER.severe("Error processing CSV upload: " + e.getMessage());
            return ApiResponseHelper.errorResponse("Error processing request: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @DeleteMapping("/{labId}/delete-all")
    public ResponseEntity<?> deleteAllTestReferences(
            @PathVariable Long labId) {
        try {
            Optional<User> userOptional = getAuthenticatedUser();
            if (userOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
            }

            User currentUser = userOptional.get();
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }

            Lab lab = labOptional.get();
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not authorized for this lab", HttpStatus.UNAUTHORIZED);
            }

            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            testReferenceServices.deleteAllTestReferences(lab);
            return ApiResponseHelper.successResponseWithDataAndMessage("All test references deleted successfully", HttpStatus.OK, null);

        } catch (Exception e) {
            LOGGER.severe("Error deleting all test references: " + e.getMessage());
            return ApiResponseHelper.errorResponse("Error processing request: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @GetMapping
    public ResponseEntity<?> getAllTestReferences(
            @RequestParam Long labId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Optional<User> userOptional = getAuthenticatedUser();
            if (userOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
            }
            User currentUser = userOptional.get();
            
            if (labId == null) {
                return ApiResponseHelper.errorResponse("Lab ID is required", HttpStatus.BAD_REQUEST);
            }
            
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }
            Lab lab = labOptional.get();
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not authorized for this lab", HttpStatus.UNAUTHORIZED);
            }

            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            Map<String, Object> paginatedResponse = testReferenceServices.getAllTestReferences(lab, page, size);
            return ApiResponseHelper.successResponseWithDataAndMessage("Test references fetched successfully", HttpStatus.OK, paginatedResponse);

        } catch (Exception e) {
            LOGGER.severe("Error fetching test references: " + e.getMessage());
            return ApiResponseHelper.errorResponse("Error processing request: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    //update test reference
    // Production-ready endpoint: Uses testReferenceCode from body (preferred) or ID from path (fallback)
    @Transactional
    @PutMapping("/{labId}/{testReferenceId}")
    public ResponseEntity<?> updateTestReference(
            @PathVariable Long labId,
            @PathVariable Long testReferenceId,
            @RequestBody TestReferenceDTO testReferenceDTO) {
        try {
            Optional<User> userOptional = getAuthenticatedUser();
            if (userOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
            }

            User currentUser = userOptional.get();
            
            // Validate DTO
            if (testReferenceDTO == null) {
                return ApiResponseHelper.errorResponse("Request body cannot be null", HttpStatus.BAD_REQUEST);
            }
            
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }

            Lab lab = labOptional.get();
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not authorized for this lab", HttpStatus.UNAUTHORIZED);
            }

            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Production-ready: Prefer testReferenceCode from DTO over path variable ID
            // If testReferenceCode is provided, use it (avoids JavaScript precision issues)
            // Otherwise, fall back to testReferenceId from path
            Long effectiveId = testReferenceId;
            if (testReferenceDTO.getTestReferenceCode() != null && !testReferenceDTO.getTestReferenceCode().trim().isEmpty()) {
                // testReferenceCode takes precedence - service will use it
                LOGGER.info("Using testReferenceCode from request body: " + testReferenceDTO.getTestReferenceCode());
            } else {
                LOGGER.info("Using testReferenceId from path: " + testReferenceId);
            }
            
            TestReferenceDTO updatedTestReference = testReferenceServices.updateTestReference(lab, effectiveId, testReferenceDTO, currentUser);
            return ApiResponseHelper.successResponseWithDataAndMessage("Test reference updated successfully", HttpStatus.OK, updatedTestReference);

        } catch (Exception e) {
            LOGGER.severe("Error updating test reference: " + e.getMessage());
            return ApiResponseHelper.errorResponse("Error processing request: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Alternative production-ready endpoint: Uses only request body (no path variable ID)
    @Transactional
    @PutMapping("/update")
    public ResponseEntity<?> updateTestReferenceByCode(
            @RequestBody TestReferenceDTO testReferenceDTO) {
        try {
            Optional<User> userOptional = getAuthenticatedUser();
            if (userOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
            }

            User currentUser = userOptional.get();
            
            // Validate DTO
            if (testReferenceDTO == null) {
                return ApiResponseHelper.errorResponse("Request body cannot be null", HttpStatus.BAD_REQUEST);
            }
            
            if (testReferenceDTO.getLabId() == null) {
                return ApiResponseHelper.errorResponse("Lab ID is required in request body", HttpStatus.BAD_REQUEST);
            }
            
            // Production-ready: Require testReferenceCode (no ID precision issues)
            if (testReferenceDTO.getTestReferenceCode() == null || testReferenceDTO.getTestReferenceCode().trim().isEmpty()) {
                return ApiResponseHelper.errorResponse("Test Reference Code is required in request body", HttpStatus.BAD_REQUEST);
            }
            
            Long labId = testReferenceDTO.getLabId();
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }

            Lab lab = labOptional.get();
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not authorized for this lab", HttpStatus.UNAUTHORIZED);
            }

            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Use null for ID since we're using testReferenceCode
            TestReferenceDTO updatedTestReference = testReferenceServices.updateTestReference(lab, null, testReferenceDTO, currentUser);
            return ApiResponseHelper.successResponseWithDataAndMessage("Test reference updated successfully", HttpStatus.OK, updatedTestReference);

        } catch (Exception e) {
            LOGGER.severe("Error updating test reference: " + e.getMessage());
            return ApiResponseHelper.errorResponse("Error processing request: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Transactional
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteTestReference(@RequestBody TestReferenceDTO testReferenceDTO) {
        try {
            Optional<User> userOptional = getAuthenticatedUser();
            if (userOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
            }

            User currentUser = userOptional.get();
            
            // Validate DTO is not null
            if (testReferenceDTO == null) {
                return ApiResponseHelper.errorResponse("Request body cannot be null", HttpStatus.BAD_REQUEST);
            }
            
            // Validate required fields
            if (testReferenceDTO.getLabId() == null) {
                return ApiResponseHelper.errorResponse("Lab ID is required in request body", HttpStatus.BAD_REQUEST);
            }
            // Production-ready: Accept either ID or testReferenceCode (prefer code to avoid precision issues)
            if (testReferenceDTO.getId() == null && (testReferenceDTO.getTestReferenceCode() == null || testReferenceDTO.getTestReferenceCode().trim().isEmpty())) {
                return ApiResponseHelper.errorResponse("Either Test Reference ID or Test Reference Code is required in request body", HttpStatus.BAD_REQUEST);
            }
            
            Long labId = testReferenceDTO.getLabId();
            Long testReferenceId = testReferenceDTO.getId();
            String testReferenceCode = testReferenceDTO.getTestReferenceCode();
            
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }

            Lab lab = labOptional.get();
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not authorized for this lab", HttpStatus.UNAUTHORIZED);
            }

            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Production-ready: Use testReferenceCode if provided, otherwise use ID
            testReferenceServices.deleteTestReference(lab, testReferenceId, testReferenceCode);
            return ApiResponseHelper.successResponseWithDataAndMessage("Test reference deleted successfully", HttpStatus.OK, null);

        } catch (Exception e) {
            LOGGER.severe("Error deleting test reference: " + e.getMessage());
            return ApiResponseHelper.errorResponse("Error processing request: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // @Transactional
    // @PostMapping("/add")
    // public ResponseEntity<?> addTestReference(@RequestBody TestReferenceDTO testReferenceDTO) {
    //     try {
    //         Optional<User> userOptional = getAuthenticatedUser();
    //         if (userOptional.isEmpty()) {
    //             return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
    //         }

    //         User currentUser = userOptional.get();
            
    //         // labId must be provided in request body
    //         if (testReferenceDTO.getLabId() == null) {
    //             return ApiResponseHelper.errorResponse("Lab ID is required in request body", HttpStatus.BAD_REQUEST);
    //         }
            
    //         Long labId = testReferenceDTO.getLabId();
    //         Optional<Lab> labOptional = labRepository.findById(labId);
    //         if (labOptional.isEmpty()) {
    //             return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
    //         }

    //         Lab lab = labOptional.get();
    //         if (!currentUser.getLabs().contains(lab)) {
    //             return ApiResponseHelper.errorResponse("User is not authorized for this lab", HttpStatus.UNAUTHORIZED);
    //         }

    //         if (!labAccessableFilter.isLabAccessible(labId)) {
    //             return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
    //         }

    //         TestReferenceDTO addedTestReference = testReferenceServices.addTestReference(lab, testReferenceDTO, currentUser);
    //         return ApiResponseHelper.successResponseWithDataAndMessage("Test reference added successfully", HttpStatus.CREATED, addedTestReference);

    //     } catch (Exception e) {
    //         LOGGER.severe("Error adding test reference: " + e.getMessage());
    //         return ApiResponseHelper.errorResponse("Error processing request: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    //     }
    // }

    @Transactional
    @PostMapping("/add")
    public ResponseEntity<?> addTestReference(@RequestBody TestReferenceDTO testReferenceDTO) {
        try {
            Optional<User> userOptional = getAuthenticatedUser();
            if (userOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
            }

            User currentUser = userOptional.get();
            
            // labId must be provided in request body
            if (testReferenceDTO.getLabId() == null) {
                return ApiResponseHelper.errorResponse("Lab ID is required in request body", HttpStatus.BAD_REQUEST);
            }
            
            Long labId = testReferenceDTO.getLabId();
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }

            Lab lab = labOptional.get();
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not authorized for this lab", HttpStatus.UNAUTHORIZED);
            }

            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            TestReferenceDTO addedTestReference = testReferenceServices.addTestReference(lab, testReferenceDTO, currentUser);
            return ApiResponseHelper.successResponseWithDataAndMessage("Test reference added successfully", HttpStatus.CREATED, addedTestReference);

        } catch (Exception e) {
            LOGGER.severe("Error adding test reference: " + e.getMessage());
            return ApiResponseHelper.errorResponse("Error processing request: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    //download test reference in csv and excel format
    @Transactional
    @GetMapping("/{labId}/download")
    public ResponseEntity<?> downloadTestReference(
            @PathVariable Long labId) {

        try {
            Optional<User> userOptional = getAuthenticatedUser();
            if (userOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
            }

            User currentUser = userOptional.get();
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }

            Lab lab = labOptional.get();
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not authorized for this lab", HttpStatus.UNAUTHORIZED);
            }

            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            ResponseEntity<?> responseEntity = testReferenceServices.downloadTestReference(lab);
            return responseEntity;

        } catch (Exception e) {
            LOGGER.severe("Error downloading test reference: " + e.getMessage());
            return ApiResponseHelper.errorResponse("Error processing request: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


    //get the list of refenance by test name will be multiple
   @Transactional
   @GetMapping("/{labId}/test/{testName:.+}") 
   public ResponseEntity<?> getTestReferenceByTestName(
           @PathVariable Long labId,
           @PathVariable String testName) {
       try {
           Optional<User> userOptional = getAuthenticatedUser();
           if (userOptional.isEmpty()) {
               return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
           }

           User currentUser = userOptional.get();
           Optional<Lab> labOptional = labRepository.findById(labId);
           if (labOptional.isEmpty()) {
               return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
           }

           Lab lab = labOptional.get();
           if (!currentUser.getLabs().contains(lab)) {
               return ApiResponseHelper.errorResponse("User is not authorized for this lab", HttpStatus.UNAUTHORIZED);
           }

           if (!labAccessableFilter.isLabAccessible(labId)) {
               return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
           }

           List<TestReferenceDTO> testReferenceEntities = testReferenceServices.getTestReferenceByTestName(lab, testName);
           return ApiResponseHelper.successResponseWithDataAndMessage("Test references fetched successfully", HttpStatus.OK, testReferenceEntities);

       } catch (Exception e) {
           LOGGER.severe("Error fetching test references: " + e.getMessage());
           return ApiResponseHelper.errorResponse("Error processing request: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
       }
   }



//    get the list of refenance by test name will be multiple
    @Transactional
    @GetMapping("/{labId}/test")
    public ResponseEntity<?> getTestReferenceByTestNameQuery(
            @PathVariable Long labId,
            @RequestParam String testName) {
        try {
            Optional<User> userOptional = getAuthenticatedUser();
            if (userOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
            }

            User currentUser = userOptional.get();
            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }

            Lab lab = labOptional.get();
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not authorized for this lab", HttpStatus.UNAUTHORIZED);
            }

            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            LOGGER.info("Searching for test name: '" + testName + "' in lab: " + labId);
            List<TestReferenceDTO> testReferenceEntities = testReferenceServices.getTestReferenceByTestName(lab, testName);
            LOGGER.info("Found " + testReferenceEntities.size() + " test references for test name: '" + testName + "'");
            return ApiResponseHelper.successResponseWithDataAndMessage("Test references fetched successfully", HttpStatus.OK, testReferenceEntities);

        } catch (Exception e) {
            LOGGER.severe("Error fetching test references: " + e.getMessage());
            return ApiResponseHelper.errorResponse("Error processing request: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
