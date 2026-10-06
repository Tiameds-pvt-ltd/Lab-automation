package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.dto.lab.DescriptionMasterDTO;
import tiameds.com.tiameds.dto.lab.ParameterMasterDTO;
import tiameds.com.tiameds.dto.lab.TestsMasterDTO;
import tiameds.com.tiameds.entity.ParameterMaster;
import tiameds.com.tiameds.entity.TestsMaster;
import tiameds.com.tiameds.repository.DescriptionMasterRepository;
import tiameds.com.tiameds.repository.ParameterMasterRepository;
import tiameds.com.tiameds.repository.TestsMasterRepository;
import tiameds.com.tiameds.services.lab.TestsMasterService;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public/tests-master")
@Tag(name = "Tests Master", description = "Public endpoints for managing the global tests master list")
public class TestsMasterController {

    private final TestsMasterRepository testsMasterRepository;
    private final TestsMasterService testsMasterService;
    private final ParameterMasterRepository parameterMasterRepository;
    private final DescriptionMasterRepository descriptionMasterRepository;

    public TestsMasterController(TestsMasterRepository testsMasterRepository,
                                 TestsMasterService testsMasterService,
                                 ParameterMasterRepository parameterMasterRepository,
                                 DescriptionMasterRepository descriptionMasterRepository) {
        this.testsMasterRepository = testsMasterRepository;
        this.testsMasterService = testsMasterService;
        this.parameterMasterRepository = parameterMasterRepository;
        this.descriptionMasterRepository = descriptionMasterRepository;
    }

    // 1. Get all active tests (simple list)
    @GetMapping
    public ResponseEntity<?> getAllTests() {
        try {
            List<TestsMasterDTO> tests = testsMasterService.getAllTests();
            return ApiResponseHelper.successResponseWithDataAndMessage("Tests master list retrieved successfully", HttpStatus.OK, tests);
        } catch (Exception e) {
            return ApiResponseHelper.successResponseWithDataAndMessage("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    // 2. Get all active tests - paginated with search, category, sort
    @GetMapping("/list")
    public ResponseEntity<?> getAllTestsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sortOrder) {
        try {
            Map<String, Object> response = testsMasterService.getAllTests(page, size, search, category, sortOrder);
            return ApiResponseHelper.successResponseWithDataAndMessage("Tests retrieved successfully", HttpStatus.OK, response);
        } catch (Exception e) {
            return ApiResponseHelper.successResponseWithDataAndMessage("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    // 3. Get active test by ID
    @GetMapping("/{testId}")
    public ResponseEntity<?> getTestById(@PathVariable Integer testId) {
        try {
            TestsMaster test = testsMasterRepository.findByTestIdAndIsActive(testId, 1).orElse(null);
            if (test == null) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Test not found", HttpStatus.NOT_FOUND, null);
            }
            return ApiResponseHelper.successResponseWithDataAndMessage("Test retrieved successfully", HttpStatus.OK, testsMasterService.toDTO(test));
        } catch (Exception e) {
            return ApiResponseHelper.successResponseWithDataAndMessage("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    // 4. Add a new test to the master list
    @Transactional
    @PostMapping("/add")
    public ResponseEntity<?> addTest(@RequestBody TestsMasterDTO testsMasterDTO) {
        try {
            if (testsMasterDTO.getName() == null || testsMasterDTO.getName().isBlank()) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Test name is required", HttpStatus.BAD_REQUEST, null);
            }
            if (testsMasterDTO.getCategory() == null || testsMasterDTO.getCategory().isBlank()) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Category is required", HttpStatus.BAD_REQUEST, null);
            }
            if (testsMasterDTO.getPrice() == null) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Price is required", HttpStatus.BAD_REQUEST, null);
            }
            if (testsMasterRepository.existsByName(testsMasterDTO.getName())) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Test with this name already exists", HttpStatus.BAD_REQUEST, null);
            }

            TestsMaster test = new TestsMaster();
            test.setName(testsMasterDTO.getName());
            test.setCategory(testsMasterDTO.getCategory());
            test.setPrice(testsMasterDTO.getPrice());
            test.setIsActive(1);
            // Save to get auto-generated ID, then set test_code based on it
            test.setTestCode("TMP-" + System.nanoTime());
            TestsMaster persisted = testsMasterRepository.save(test);
            persisted.setTestCode(String.format("MSTR-%05d", persisted.getTestId()));
            TestsMaster saved = testsMasterRepository.save(persisted);
            return ApiResponseHelper.successResponseWithDataAndMessage("Test added successfully", HttpStatus.CREATED, testsMasterService.toDTO(saved));
        } catch (Exception e) {
            return ApiResponseHelper.successResponseWithDataAndMessage("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    // 5. Update an existing active test
    @Transactional
    @PutMapping("/update/{testId}")
    public ResponseEntity<?> updateTest(@PathVariable Integer testId, @RequestBody TestsMasterDTO testsMasterDTO) {
        try {
            TestsMaster test = testsMasterRepository.findByTestIdAndIsActive(testId, 1).orElse(null);
            if (test == null) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Test not found", HttpStatus.NOT_FOUND, null);
            }
            if (testsMasterDTO.getTestCode() != null && !testsMasterDTO.getTestCode().equals(test.getTestCode())) {
                if (testsMasterRepository.existsByTestCode(testsMasterDTO.getTestCode())) {
                    return ApiResponseHelper.successResponseWithDataAndMessage("Test with this code already exists", HttpStatus.BAD_REQUEST, null);
                }
                test.setTestCode(testsMasterDTO.getTestCode());
            }
            if (testsMasterDTO.getName() != null && !testsMasterDTO.getName().isBlank()) {
                if (!testsMasterDTO.getName().equals(test.getName()) && testsMasterRepository.existsByName(testsMasterDTO.getName())) {
                    return ApiResponseHelper.successResponseWithDataAndMessage("Test with this name already exists", HttpStatus.BAD_REQUEST, null);
                }
                test.setName(testsMasterDTO.getName());
            }
            if (testsMasterDTO.getCategory() != null && !testsMasterDTO.getCategory().isBlank()) {
                test.setCategory(testsMasterDTO.getCategory());
            }
            if (testsMasterDTO.getPrice() != null) {
                test.setPrice(testsMasterDTO.getPrice());
            }

            TestsMaster updated = testsMasterRepository.save(test);
            return ApiResponseHelper.successResponseWithDataAndMessage("Test updated successfully", HttpStatus.OK, testsMasterService.toDTO(updated));
        } catch (Exception e) {
            return ApiResponseHelper.successResponseWithDataAndMessage("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    // 6. Soft delete: set is_active = 0
    @Transactional
    @DeleteMapping("/remove/{testId}")
    public ResponseEntity<?> removeTest(@PathVariable Integer testId) {
        try {
            TestsMaster test = testsMasterRepository.findByTestIdAndIsActive(testId, 1).orElse(null);
            if (test == null) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Test not found or already inactive", HttpStatus.NOT_FOUND, null);
            }
            test.setIsActive(0);
            testsMasterRepository.save(test);
            return ApiResponseHelper.successResponseWithDataAndMessage("Test deactivated successfully", HttpStatus.OK, null);
        } catch (Exception e) {
            return ApiResponseHelper.successResponseWithDataAndMessage("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    // 7. Bulk upload via CSV  (columns: Category Name, LabTest Name, Price(INR))
    @Transactional
    @PostMapping("/csv/upload")
    public ResponseEntity<?> uploadCSV(@RequestParam("file") MultipartFile file) {
        try {
            String contentType = file.getContentType();
            if (file.isEmpty() || contentType == null || !contentType.equals("text/csv")) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Please upload a CSV file", HttpStatus.BAD_REQUEST, null);
            }

            List<TestsMaster> tests = testsMasterService.uploadCSV(file);
            List<TestsMasterDTO> testDTOs = tests.stream()
                    .map(testsMasterService::toDTO)
                    .collect(Collectors.toList());

            return ApiResponseHelper.successResponseWithDataAndMessage("Tests uploaded successfully", HttpStatus.CREATED, testDTOs);
        } catch (RuntimeException e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 8. Download all active tests as CSV
    @GetMapping("/download")
    public ResponseEntity<?> downloadCSV() {
        try {
            return testsMasterService.downloadCSV();
        } catch (RuntimeException e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 9. Get all parameters for a specific test
    @GetMapping("/{testId}/parameters")
    public ResponseEntity<?> getParametersByTest(@PathVariable Integer testId) {
        try {
            TestsMaster test = testsMasterRepository.findByTestIdAndIsActive(testId, 1).orElse(null);
            if (test == null) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Test not found", HttpStatus.NOT_FOUND, null);
            }

            List<ParameterMasterDTO> parameters = parameterMasterRepository.findAllByTest(test)
                    .stream()
                    .map(p -> new ParameterMasterDTO(p.getParameterId(), p.getParameterName(), testId))
                    .collect(Collectors.toList());

            return ApiResponseHelper.successResponseWithDataAndMessage("Parameters retrieved successfully", HttpStatus.OK, parameters);
        } catch (Exception e) {
            return ApiResponseHelper.successResponseWithDataAndMessage("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    // 10. Get all descriptions for a specific parameter
    @GetMapping("/{testId}/parameters/{parameterId}/descriptions")
    public ResponseEntity<?> getDescriptionsByParameter(@PathVariable Integer testId,
                                                        @PathVariable Integer parameterId) {
        try {
            TestsMaster test = testsMasterRepository.findByTestIdAndIsActive(testId, 1).orElse(null);
            if (test == null) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Test not found", HttpStatus.NOT_FOUND, null);
            }

            ParameterMaster parameter = parameterMasterRepository.findById(parameterId).orElse(null);
            if (parameter == null || !parameter.getTest().getTestId().equals(testId)) {
                return ApiResponseHelper.successResponseWithDataAndMessage("Parameter not found for this test", HttpStatus.NOT_FOUND, null);
            }

            List<DescriptionMasterDTO> descriptions = descriptionMasterRepository.findAllByParameter(parameter)
                    .stream()
                    .map(d -> new DescriptionMasterDTO(
                            d.getDescriptionId(),
                            d.getDescriptionName(),
                            d.getResultType(),
                            d.getOptions(),
                            parameterId))
                    .collect(Collectors.toList());

            return ApiResponseHelper.successResponseWithDataAndMessage("Descriptions retrieved successfully", HttpStatus.OK, descriptions);
        } catch (Exception e) {
            return ApiResponseHelper.successResponseWithDataAndMessage("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }
}
