package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.dto.lab.TestDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.Test;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.TestRepository;
import tiameds.com.tiameds.services.lab.TestServices;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/lab")
@Tag(name = "Lab Tests", description = "Endpoints for managing lab tests")
public class TestController {

    private final LabRepository labRepository;
    private final TestRepository testRepository;
    private final UserAuthService userAuthService;
    private final LabAccessableFilter labAccessableFilter;
    private final TestServices testServices;

    public TestController(LabRepository labRepository, TestRepository testRepository, UserAuthService userAuthService, LabAccessableFilter labAccessableFilter, TestServices testServices) {
        this.labRepository = labRepository;
        this.testRepository = testRepository;
        this.userAuthService = userAuthService;
        this.labAccessableFilter = labAccessableFilter;
        this.testServices = testServices;
    }


    // 1. Get all tests in a lab
    @GetMapping("/{labId}/tests")
    public ResponseEntity<?> getAllTests(
            @PathVariable Long labId,
            @RequestHeader("Authorization") String token) {
        try {
            // Authenticate the user using the provided token
            User currentUser = userAuthService.authenticateUser(token)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if the lab exists in the repository
            Lab lab = labRepository.findById(labId)
                    .orElseThrow(() -> new RuntimeException("Lab not found"));

            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Lab is not accessible", HttpStatus.UNAUTHORIZED, null);
            }

            // Verify if the current user is associated with the lab
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.successResponseWithDataAndMessage("User is not a member of this lab", HttpStatus.UNAUTHORIZED, null);
            }

            // Retrieve, sort by ID in ascending order, and map all tests to DTOs
            List<TestDTO> testDTOs = lab.getTests().stream()
                    .sorted(Comparator.comparingLong(Test::getId)) // Sort by ID in ascending order
                    .map(test -> new TestDTO(
                            test.getId(),
                            test.getCategory(),
                            test.getName(),
                            test.getPrice(),
                            test.getCreatedAt(),
                            test.getUpdatedAt()
                    ))
                    .collect(Collectors.toList());

            return ApiResponseHelper.successResponseWithDataAndMessage("Tests retrieved successfully", HttpStatus.OK, testDTOs);

        } catch (Exception e) {
            // Handle unexpected exceptions and provide meaningful error messages
            return ApiResponseHelper.errorResponse("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 2. Add a new test to a lab
    @PostMapping("/{labId}/add")
    public ResponseEntity<?> addTest(
            @PathVariable Long labId,
            @RequestBody TestDTO testDTO,
            @RequestHeader("Authorization") String token) {
        try {
            // Authenticate the user using the provided token
            User currentUser = userAuthService.authenticateUser(token)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if the lab exists in the repository
            Lab lab = labRepository.findById(labId)
                    .orElseThrow(() -> new RuntimeException("Lab not found"));


            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (!isAccessible) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Lab is not accessible", HttpStatus.UNAUTHORIZED, null);
            }


            // Verify if the current user is associated with the lab
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.successResponseWithDataAndMessage("User is not a member of this lab", HttpStatus.UNAUTHORIZED, null);
            }


            // check the test is already in the  the particular lab
            Set<Test> tests = lab.getTests();
            for (Test test : tests) {
                if (test.getName().equals(testDTO.getName())) {
                    return ApiResponseHelper.successResponseWithDataAndMessage("Test already exists in the lab", HttpStatus.BAD_REQUEST, null);
                }
            }

            // Create a new Test entity from the DTO
            Test test = new Test();
            test.setCategory(testDTO.getCategory());
            test.setName(testDTO.getName());
            test.setPrice(testDTO.getPrice());

            // Add the test to the lab and maintain the bidirectional relationship
            lab.addTest(test);

            // Persist the new Test entity and update the Lab entity
            labRepository.save(lab); // This will cascade and save the Test entity if properly configured

            // Optionally, map the saved Test back to a DTO to include generated data like ID
            TestDTO savedTestDTO = new TestDTO(
                    test.getId(),
                    test.getCategory(),
                    test.getName(),
                    test.getPrice(),
                    test.getCreatedAt(),
                    test.getUpdatedAt()
            );

            return ApiResponseHelper.successResponseWithDataAndMessage("Test added successfully", HttpStatus.CREATED, savedTestDTO);

        } catch (Exception e) {
            // Handle unexpected exceptions and provide meaningful error messages
            return ApiResponseHelper.successResponseWithDataAndMessage("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }


    // 3. Update a test in a lab by ID only if test id and lab id are matching
    @PutMapping("/{labId}/update/{testId}")
    public ResponseEntity<?> updateTest(
            @PathVariable Long labId,
            @PathVariable Long testId,
            @RequestBody TestDTO testDTO,
            @RequestHeader("Authorization") String token) {
        try {
            // Authenticate the user using the provided token
            User currentUser = userAuthService.authenticateUser(token)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if the lab exists in the repository
            Lab lab = labRepository.findById(labId)
                    .orElseThrow(() -> new RuntimeException("Lab not found"));

            // Verify if the current user is associated with the lab
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.successResponseWithDataAndMessage("User is not a member of this lab", HttpStatus.UNAUTHORIZED, null);
            }


            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Lab is not accessible", HttpStatus.UNAUTHORIZED, null);
            }

            // Check if the test exists in the repository
            Test test = testRepository.findById(testId)
                    .orElseThrow(() -> new RuntimeException("Test not found"));

            // Check if the test belongs to the lab
            if (!lab.getTests().contains(test)) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Test does not belong to this lab", HttpStatus.BAD_REQUEST, null);
            }

            // Update the test entity with the new data
            test.setCategory(testDTO.getCategory());
            test.setName(testDTO.getName());
            test.setPrice(testDTO.getPrice());

            // Persist the updated Test entity
            testRepository.save(test);

            // Optionally, map the updated Test back to a DTO to include generated data like ID
            TestDTO updatedTestDTO = new TestDTO(
                    test.getId(),
                    test.getCategory(),
                    test.getName(),
                    test.getPrice(),
                    test.getCreatedAt(),
                    test.getUpdatedAt()
            );

            return ApiResponseHelper.successResponseWithDataAndMessage("Test updated successfully", HttpStatus.OK, updatedTestDTO);

        } catch (Exception e) {
            // Handle unexpected exceptions and provide meaningful error messages
            return ApiResponseHelper.successResponseWithDataAndMessage("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }


    // 4 get test by id only if test id and lab id are matching
    @GetMapping("/{labId}/test/{testId}")
    public ResponseEntity<?> getTest(
            @PathVariable Long labId,
            @PathVariable Long testId,
            @RequestHeader("Authorization") String token) {
        try {
            // Authenticate the user using the provided token
            User currentUser = userAuthService.authenticateUser(token)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if the lab exists in the repository
            Lab lab = labRepository.findById(labId)
                    .orElseThrow(() -> new RuntimeException("Lab not found"));


            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Lab is not accessible", HttpStatus.UNAUTHORIZED, null);
            }

            // Verify if the current user is associated with the lab
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.successResponseWithDataAndMessage("User is not a member of this lab", HttpStatus.UNAUTHORIZED, null);
            }

            // Check if the test exists in the repository
            Test test = testRepository.findById(testId)
                    .orElseThrow(() -> new RuntimeException("Test not found"));

            // Check if the test belongs to the lab
            if (!lab.getTests().contains(test)) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Test does not belong to this lab", HttpStatus.BAD_REQUEST, null);
            }

            // Map the test to a DTO
            TestDTO testDTO = new TestDTO(
                    test.getId(),
                    test.getCategory(),
                    test.getName(),
                    test.getPrice(),
                    test.getCreatedAt(),
                    test.getUpdatedAt()
            );

            return ApiResponseHelper.successResponseWithDataAndMessage("Test retrieved successfully", HttpStatus.OK, testDTO);

        } catch (Exception e) {
            // Handle unexpected exceptions and provide meaningful error messages
            return ApiResponseHelper.successResponseWithDataAndMessage("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }

    }


    // 5. delete a test from a lab by ID only if test id and lab id are matching
    @DeleteMapping("/{labId}/remove/{testId}")
    @Transactional
    public ResponseEntity<?> removeTest(
            @PathVariable Long labId,
            @PathVariable Long testId,
            @RequestHeader("Authorization") String token) {
        try {
            // Authenticate the user using the provided token
            User currentUser = userAuthService.authenticateUser(token)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if the lab exists in the repository
            Lab lab = labRepository.findById(labId)
                    .orElseThrow(() -> new RuntimeException("Lab not found"));

            // Verify if the current user is associated with the lab
            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
            }

            // Check if the lab is active
            boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
            if (isAccessible == false) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            // Check if the test exists in the repository
            Test test = testRepository.findById(testId)
                    .orElseThrow(() -> new RuntimeException("Test not found"));

            // Check if the test belongs to the lab
            if (!lab.getTests().contains(test)) {
                return ApiResponseHelper.errorResponse("Test does not belong to this lab", HttpStatus.BAD_REQUEST);
            }

            // Remove the test from the lab and maintain the bidirectional relationship
            lab.removeTest(test);

            //delete the test
            testRepository.deleteById(testId);

            // Persist the updated Lab entity
            labRepository.save(lab);

            return ResponseEntity.ok(ApiResponseHelper.successResponse("Test removed successfully", null).getBody());

        } catch (Exception e) {
            // Handle unexpected exceptions and provide meaningful error messages
            return ApiResponseHelper.successResponseWithDataAndMessage("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    //6 upload csv
    @PostMapping("/test/{labId}/csv/upload")
    public ResponseEntity<?> uploadCSV(
            @PathVariable Long labId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {
        try {
            // Authenticate the user
            User currentUser = userAuthService.authenticateUser(token)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify lab existence and user association
            Lab lab = labRepository.findById(labId)
                    .orElseThrow(() -> new RuntimeException("Lab not found"));

            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.successResponseWithDataAndMessage("User is not authorized for this lab", HttpStatus.UNAUTHORIZED, null);
            }

            // Verify lab accessibility
            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Lab is not accessible", HttpStatus.UNAUTHORIZED, null);
            }

            // Validate file type
            if (file.isEmpty() || !file.getContentType().equals("text/csv")) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Please upload a CSV file", HttpStatus.BAD_REQUEST, null);
            }

            // Process the file and save tests
            List<Test> tests = testServices.uploadCSV(file, lab);

            // Convert saved tests to DTOs for response
            List<TestDTO> testDTOs = tests.stream()
                    .map(test -> new TestDTO(
                            test.getId(),
                            test.getCategory(),
                            test.getName(),
                            test.getPrice(),
                            test.getCreatedAt(),
                            test.getUpdatedAt()
                    ))
                    .collect(Collectors.toList());

            return ApiResponseHelper.successResponseWithDataAndMessage("Tests uploaded successfully", HttpStatus.CREATED, testDTOs);

        } catch (RuntimeException e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // 7 download csv file of respective lab tests only
    @GetMapping("/{labId}/download")
    public ResponseEntity<?> downloadCSV(
            @PathVariable Long labId,
            @RequestHeader("Authorization") String token) {
        try {
            // Authenticate the user
            User currentUser = userAuthService.authenticateUser(token)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify lab existence and user association
            Lab lab = labRepository.findById(labId)
                    .orElseThrow(() -> new RuntimeException("Lab not found"));

            if (!currentUser.getLabs().contains(lab)) {
                return ApiResponseHelper.successResponseWithDataAndMessage("User is not authorized for this lab", HttpStatus.UNAUTHORIZED, null);
            }

            // Verify lab accessibility
            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Lab is not accessible", HttpStatus.UNAUTHORIZED, null);
            }

            // Generate CSV file and return as attachment
            return testServices.downloadCSV(lab);

        } catch (RuntimeException e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
