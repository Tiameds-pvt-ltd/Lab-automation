package tiameds.com.tiameds.services.lab;

import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.dto.lab.TestDTO;
import tiameds.com.tiameds.dto.lab.TestReferenceDTO;
import tiameds.com.tiameds.entity.*;
import tiameds.com.tiameds.repository.SuperAdminReferanceRepository;
import tiameds.com.tiameds.repository.SuperAdminTestRepository;
import tiameds.com.tiameds.utils.ApiResponse;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties.UiService.LOGGER;

@Service
public class AdminTestReferanceandTestServices {

    private final SuperAdminTestRepository superAdminTestRepository;
    private final SuperAdminReferanceRepository superAdminReferanceRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminTestReferanceandTestServices.class);

    public AdminTestReferanceandTestServices(SuperAdminTestRepository superAdminTestRepository, SuperAdminReferanceRepository superAdminReferanceRepository) {
        this.superAdminTestRepository = superAdminTestRepository;
        this.superAdminReferanceRepository = superAdminReferanceRepository;
    }

    @Transactional
    public void uploadTestDataPriceList(SuperAdminReferanceEntity superAdminReferanceEntity, MultipartFile file, User currentUser) {
        // List to store tests to be saved
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            // Parse CSV file with headers
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            // Process each record in the CSV file
            for (CSVRecord record : csvParser) {
                // Fetch and validate required fields
                String category = record.get("Category Name");
                String name = record.get("LabTest Name");
                String priceString = record.get("Price(INR)");
                if (category == null || name == null || priceString == null) {
                    throw new IllegalArgumentException("Missing required fields in CSV: " + record);
                }
                BigDecimal price;
                try {
                    price = new BigDecimal(priceString);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid price format in CSV: " + priceString);
                }
                // Create and populate Test entity
                SuperAdminTestEntity superAdminTestEntity = new SuperAdminTestEntity();
                superAdminTestEntity.setCategory(category);
                superAdminTestEntity.setName(name);
                superAdminTestEntity.setPrice(price);
                superAdminTestEntity.setCreatedBy(currentUser.getUsername());
                superAdminTestEntity.setUpdatedBy(currentUser.getUsername());
                superAdminTestEntity.setCreatedAt(java.time.LocalDateTime.now());
                superAdminTestEntity.setUpdatedAt(java.time.LocalDateTime.now());
                // Save the test entity
                superAdminTestRepository.save(superAdminTestEntity);
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid data in CSV file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error processing CSV file: " + e.getMessage(), e);
        }
    }

    @Transactional
    public ResponseEntity<?> downloadPriceListCSV() {
        List<SuperAdminTestEntity> tests = superAdminTestRepository.findAll();
        // Generate CSV content
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Category Name,LabTest Name,Price(INR)\n");

        for (SuperAdminTestEntity test : tests) {
            // Ensure proper formatting by escaping commas in values
            csvContent.append("\"").append(test.getCategory().replace("\"", "\"\"")).append("\",");
            csvContent.append("\"").append(test.getName().replace("\"", "\"\"")).append("\",");
            csvContent.append("\"").append(test.getPrice()).append("\"\n");
        }
        // Set the response headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=price_list.csv");
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");
        // Return the CSV content as a ResponseEntity
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent.toString());
    }

    @Transactional
    public List<SuperAdminReferanceEntity> uploadTestReferance(MultipartFile file, User currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("User authentication failed.");
        }
        List<SuperAdminReferanceEntity> testReferenceEntities = new ArrayList<>();

        try {
            byte[] fileBytes = file.getBytes();
            // Force UTF-8 encoding
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                         .withFirstRecordAsHeader()
                         .withIgnoreHeaderCase()
                         .withTrim())) {

                for (CSVRecord record : csvParser) {
                    SuperAdminReferanceEntity entity = processRecord(record, currentUser);
                    testReferenceEntities.add(superAdminReferanceRepository.save(entity));
                }
            }
            return testReferenceEntities;
        } catch (Exception e) {
            LOGGER.error("Failed to process CSV file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process CSV file: " + e.getMessage(), e);
        }
    }

    private SuperAdminReferanceEntity processRecord(CSVRecord record, User currentUser) {
        // Required fields validation
        String category = getStringOrBlank(record, "Category");
        String testName = getStringOrBlank(record, "Test Name");
        if (category.isEmpty() || testName.isEmpty()) {
            throw new IllegalArgumentException("Record #" + record.getRecordNumber() + ": Category and Test Name are required");
        }

        SuperAdminReferanceEntity entity = new SuperAdminReferanceEntity();
        entity.setCategory(category);
        entity.setTestName(testName);
        entity.setTestDescription(getStringOrBlank(record, "Test Description"));
        entity.setUnits(getStringOrBlank(record, "Units"));

        // Handle Gender (optional)
        parseGender(record, entity);

        // Reference ranges (nullable Double)
        entity.setMinReferenceRange(parseDoubleOrNull(record, "Min Reference Range"));
        entity.setMaxReferenceRange(parseDoubleOrNull(record, "Max Reference Range"));

        // Age handling with DEFAULTS
        entity.setAgeMin(parseIntWithDefault(record, "Age Min", 0));          // Default: 0
        entity.setAgeMax(parseIntWithDefault(record, "Age Max", 100));       // Default: 100
        entity.setMinAgeUnit(parseAgeUnitWithDefault(record, "Min Age Unit")); // Default: YEARS
        entity.setMaxAgeUnit(parseAgeUnitWithDefault(record, "Max Age Unit")); // Default: YEARS

        // Handle Remarks, ReportJson, and ReferenceRanges
        entity.setRemarks(getStringOrBlank(record, "Remarks"));
        
        // Process ReportJson
        String reportJson = getStringOrBlank(record, "ReportJson");
        if (!reportJson.isEmpty()) {
            // Validate JSON format
            if (isValidJson(reportJson)) {
                entity.setReportJson(reportJson.trim());
            } else {
                LOGGER.warn("Invalid JSON in ReportJson column (record {}): {}", record.getRecordNumber(), reportJson);
                entity.setReportJson(null);
            }
        } else {
            entity.setReportJson(null);
        }
        
        // Process ReferenceRanges
        String referenceRanges = getStringOrBlank(record, "ReferenceRanges");
        if (!referenceRanges.isEmpty()) {
            // Validate JSON array format
            if (isValidJsonArray(referenceRanges)) {
                entity.setReferenceRanges(referenceRanges.trim());
            } else {
                LOGGER.warn("Invalid JSON array in ReferenceRanges column (record {}): {}", record.getRecordNumber(), referenceRanges);
                entity.setReferenceRanges(null);
            }
        } else {
            entity.setReferenceRanges(null);
        }

        // Audit fields
        entity.setCreatedBy(currentUser.getUsername());
        entity.setUpdatedBy(currentUser.getUsername());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        return entity;
    }

    // ------------------ Helper Methods ------------------

    private String getStringOrBlank(CSVRecord record, String column) {
        try {
            String value = record.get(column);
            return (value == null) ? "" : value.trim();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private Double parseDoubleOrNull(CSVRecord record, String column) {
        String value = getStringOrBlank(record, column);
        if (value.isEmpty()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid number in column '{}' (record {}): {}", column, record.getRecordNumber(), e.getMessage());
            return null;
        }
    }

    private Integer parseIntWithDefault(CSVRecord record, String column, int defaultValue) {
        String value = getStringOrBlank(record, column);
        if (value.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid integer in column '{}' (record {}). Using default: {}",
                    column, record.getRecordNumber(), defaultValue);
            return defaultValue;
        }
    }

    private AgeUnit parseAgeUnitWithDefault(CSVRecord record, String column) {
        String value = getStringOrBlank(record, column);
        if (value.isEmpty()) return AgeUnit.YEARS; // Default unit
        try {
            return AgeUnit.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid age unit '{}' (record {}). Defaulting to YEARS",
                    value, record.getRecordNumber());
            return AgeUnit.YEARS;
        }
    }

    private void parseGender(CSVRecord record, SuperAdminReferanceEntity entity) {
        String genderStr = getStringOrBlank(record, "Gender");
        if (!genderStr.isEmpty()) {
            genderStr = genderStr.trim().toUpperCase();

            switch (genderStr) {
                case "M":
                    entity.setGender(Gender.M);
                    break;
                case "F":
                    entity.setGender(Gender.F);
                    break;
                case "M/F":
                case "MF":
                    entity.setGender(Gender.MF);
                    break;
                default:
                    LOGGER.warn("Unknown gender '{}' (record {}). Defaulting to MF.", genderStr, record.getRecordNumber());
                    entity.setGender(Gender.MF);
            }
        } else {
            // Default to MF when gender is empty
            entity.setGender(Gender.MF);
        }
    }



    /**
     * Detect charset using simple heuristic.
     * - Tries UTF-8 validation first
     * - Falls back to Windows-1252 (Excel default on Windows)
     */
    private Charset detectCharset(byte[] bytes) {
        try {
            String s = new String(bytes, StandardCharsets.UTF_8);
            byte[] encoded = s.getBytes(StandardCharsets.UTF_8);
            if (Arrays.equals(encoded, bytes)) {
                return StandardCharsets.UTF_8;
            }
        } catch (Exception ignore) {
        }
        return Charset.forName("Windows-1252");
    }

    //----------------------------------------------------------------------------------------
    public ResponseEntity<?> downloadTestReferanceCSV() {
        List<SuperAdminReferanceEntity> testReferences = superAdminReferanceRepository.findAll();
        // Generate CSV content
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Category,Test Name,Test Description,Units,Gender,Min Reference Range,Max Reference Range,Age Min,Min Age Unit,Age Max,Max Age Unit,Remarks,ReportJson,ReferenceRanges\n");
        for (SuperAdminReferanceEntity testReference : testReferences) {
            csvContent.append("\"").append(escapeCsv(testReference.getCategory())).append("\",");
            csvContent.append("\"").append(escapeCsv(testReference.getTestName())).append("\",");
            csvContent.append("\"").append(escapeCsv(testReference.getTestDescription())).append("\",");
            csvContent.append("\"").append(escapeCsv(testReference.getUnits())).append("\",");
            csvContent.append("\"").append(escapeCsv(String.valueOf(testReference.getGender()))).append("\",");

            csvContent.append(testReference.getMinReferenceRange() != null ? testReference.getMinReferenceRange() : "").append(",");
            csvContent.append(testReference.getMaxReferenceRange() != null ? testReference.getMaxReferenceRange() : "").append(",");
            csvContent.append(testReference.getAgeMin() != null ? testReference.getAgeMin() : "").append(",");
            csvContent.append("\"").append(escapeCsv(String.valueOf(testReference.getMinAgeUnit()))).append("\",");
            csvContent.append(testReference.getAgeMax() != null ? testReference.getAgeMax() : "").append(",");
            csvContent.append("\"").append(escapeCsv(String.valueOf(testReference.getMaxAgeUnit()))).append("\",");
            csvContent.append("\"").append(escapeCsv(testReference.getRemarks())).append("\",");
            csvContent.append("\"").append(escapeCsv(testReference.getReportJson())).append("\",");
            csvContent.append("\"").append(escapeCsv(testReference.getReferenceRanges())).append("\"\n");
        }

        // Set the response headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=test_references.csv");
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent.toString());
    }

    // Utility method to escape double quotes in CSV
    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    public ResponseEntity<?> getAllTestPriceList() {
        List<SuperAdminTestEntity> testPriceList = superAdminTestRepository.findAll();
        if (testPriceList.isEmpty()) {
            return ApiResponseHelper.errorResponse("No test price list found", HttpStatus.NOT_FOUND);
        }
        List<TestDTO> testDTOs = testPriceList.stream()
                .map(test -> {
                    TestDTO dto = new TestDTO();
                    dto.setId(test.getId());
                    dto.setCategory(test.getCategory());
                    dto.setName(test.getName());
                    dto.setPrice(test.getPrice());
                    dto.setCreatedAt(test.getCreatedAt());
                    dto.setUpdatedAt(test.getUpdatedAt());
                    return dto;
                })
                .sorted(Comparator.comparing(TestDTO::getCategory).thenComparing(TestDTO::getName))
                .collect(Collectors.toList());
        if (testPriceList.isEmpty()) {
            return ApiResponseHelper.errorResponse("No test price list found", HttpStatus.NOT_FOUND);
        } else {
            return ApiResponseHelper.successResponse("Test price list retrieved successfully", testDTOs);
        }
    }

    public ResponseEntity<?> getAllTestReferance() {
        List<SuperAdminReferanceEntity> testReferences = superAdminReferanceRepository.findAll();
        if (testReferences.isEmpty()) {
            return ApiResponseHelper.errorResponse("No test references found", HttpStatus.NOT_FOUND);
        }
        List<TestReferenceDTO> testReferenceDTOs = testReferences.stream()
                .map(testReference -> {
                    TestReferenceDTO dto = new TestReferenceDTO();
                    dto.setId(testReference.getId());
                    dto.setCategory(testReference.getCategory());
                    dto.setTestName(testReference.getTestName());
                    dto.setTestDescription(testReference.getTestDescription());
                    dto.setUnits(testReference.getUnits());
                    dto.setGender(testReference.getGender());
                    dto.setMinReferenceRange(testReference.getMinReferenceRange());
                    dto.setMaxReferenceRange(testReference.getMaxReferenceRange());
                    dto.setAgeMin(testReference.getAgeMin());
                    dto.setMinAgeUnit(String.valueOf(testReference.getMinAgeUnit()));
                    dto.setAgeMax(testReference.getAgeMax());
                    dto.setMaxAgeUnit(String.valueOf(testReference.getMaxAgeUnit()));
                    dto.setCreatedAt(testReference.getCreatedAt());
                    dto.setUpdatedAt(testReference.getUpdatedAt());
                    return dto;
                })
                .sorted(Comparator.comparing(TestReferenceDTO::getCategory).thenComparing(TestReferenceDTO::getTestName))
                .collect(Collectors.toList());
        return ApiResponseHelper.successResponse("Test references retrieved successfully", testReferenceDTOs);
    }

    // ------------------ JSON Validation Helper ------------------
    
    private boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        try {
            // Simple JSON validation - check if it starts with { and ends with }
            String trimmed = jsonString.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                // Additional validation could be added here using Jackson ObjectMapper
                // For now, basic structure validation is sufficient
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.warn("JSON validation error: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean isValidJsonArray(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        try {
            // Simple JSON array validation - check if it starts with [ and ends with ]
            String trimmed = jsonString.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                // Additional validation could be added here using Jackson ObjectMapper
                // For now, basic structure validation is sufficient
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.warn("JSON array validation error: {}", e.getMessage());
            return false;
        }
    }

    // ------------------ JSON-Specific Operations ------------------

    @Transactional
    public ResponseEntity<?> createTestReferenceWithJson(SuperAdminReferanceEntity testReference, User currentUser) {
        try {
            // Validate JSON if provided
            if (testReference.getReportJson() != null && !testReference.getReportJson().trim().isEmpty()) {
                if (!isValidJson(testReference.getReportJson())) {
                    return ApiResponseHelper.errorResponse("Invalid JSON format in ReportJson field", HttpStatus.BAD_REQUEST);
                }
            }

            // Set audit fields
            testReference.setCreatedBy(currentUser.getUsername());
            testReference.setUpdatedBy(currentUser.getUsername());
            testReference.setCreatedAt(LocalDateTime.now());
            testReference.setUpdatedAt(LocalDateTime.now());

            // Save the entity
            superAdminReferanceRepository.save(testReference);
            return ApiResponseHelper.successResponseWithData("Test reference created successfully with JSON data");
        } catch (Exception e) {
            LOGGER.error("Error creating test reference with JSON: {}", e.getMessage(), e);
            return ApiResponseHelper.errorResponse("Error creating test reference: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public ResponseEntity<?> updateTestReferenceJson(Long id, String reportJson, User currentUser) {
        try {
            SuperAdminReferanceEntity entity = superAdminReferanceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Test reference not found with ID: " + id));

            // Validate JSON if provided
            if (reportJson != null && !reportJson.trim().isEmpty()) {
                if (!isValidJson(reportJson)) {
                    return ApiResponseHelper.errorResponse("Invalid JSON format", HttpStatus.BAD_REQUEST);
                }
                entity.setReportJson(reportJson);
            } else {
                entity.setReportJson(null);
            }

            // Update audit fields
            entity.setUpdatedBy(currentUser.getUsername());
            entity.setUpdatedAt(LocalDateTime.now());

            superAdminReferanceRepository.save(entity);
            return ApiResponseHelper.successResponseWithData("Test reference JSON updated successfully");
        } catch (Exception e) {
            LOGGER.error("Error updating test reference JSON: {}", e.getMessage(), e);
            return ApiResponseHelper.errorResponse("Error updating test reference JSON: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> searchTestReferencesByJson(String jsonKey, String jsonValue) {
        try {
            List<SuperAdminReferanceEntity> allReferences = superAdminReferanceRepository.findAll();
            List<SuperAdminReferanceEntity> filteredReferences = new ArrayList<>();

            for (SuperAdminReferanceEntity reference : allReferences) {
                if (reference.getReportJson() != null && !reference.getReportJson().trim().isEmpty()) {
                    // Simple search - check if the JSON contains the key-value pair
                    String json = reference.getReportJson().toLowerCase();
                    if (jsonKey != null && jsonValue != null) {
                        if (json.contains(jsonKey.toLowerCase()) && json.contains(jsonValue.toLowerCase())) {
                            filteredReferences.add(reference);
                        }
                    } else if (jsonKey != null) {
                        if (json.contains(jsonKey.toLowerCase())) {
                            filteredReferences.add(reference);
                        }
                    } else if (jsonValue != null) {
                        if (json.contains(jsonValue.toLowerCase())) {
                            filteredReferences.add(reference);
                        }
                    }
                }
            }

            return ApiResponseHelper.successResponse("Test references found", filteredReferences);
        } catch (Exception e) {
            LOGGER.error("Error searching test references by JSON: {}", e.getMessage(), e);
            return ApiResponseHelper.errorResponse("Error searching test references: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> getTestReferenceById(Long id) {
        try {
            SuperAdminReferanceEntity reference = superAdminReferanceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Test reference not found with ID: " + id));
            return ApiResponseHelper.successResponse("Test reference retrieved successfully", reference);
        } catch (Exception e) {
            LOGGER.error("Error retrieving test reference by ID: {}", e.getMessage(), e);
            return ApiResponseHelper.errorResponse("Error retrieving test reference: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public ResponseEntity<?> updateTestReferenceRanges(Long id, String referenceRanges, User currentUser) {
        try {
            SuperAdminReferanceEntity entity = superAdminReferanceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Test reference not found with ID: " + id));

            // Validate JSON array if provided
            if (referenceRanges != null && !referenceRanges.trim().isEmpty()) {
                if (!isValidJsonArray(referenceRanges)) {
                    return ApiResponseHelper.errorResponse("Invalid JSON array format", HttpStatus.BAD_REQUEST);
                }
                entity.setReferenceRanges(referenceRanges.trim());
            } else {
                entity.setReferenceRanges(null);
            }

            // Update audit fields
            entity.setUpdatedBy(currentUser.getUsername());
            entity.setUpdatedAt(LocalDateTime.now());

            superAdminReferanceRepository.save(entity);
            return ApiResponseHelper.successResponseWithData("Test reference ranges updated successfully");
        } catch (Exception e) {
            LOGGER.error("Error updating test reference ranges: {}", e.getMessage(), e);
            return ApiResponseHelper.errorResponse("Error updating test reference ranges: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> searchTestReferencesByReferenceRanges(String gender, String ageMin, String ageMax) {
        try {
            List<SuperAdminReferanceEntity> allReferences = superAdminReferanceRepository.findAll();
            List<SuperAdminReferanceEntity> filteredReferences = new ArrayList<>();

            for (SuperAdminReferanceEntity reference : allReferences) {
                if (reference.getReferenceRanges() != null && !reference.getReferenceRanges().trim().isEmpty()) {
                    String referenceRanges = reference.getReferenceRanges().toLowerCase();
                    boolean matches = true;

                    // Search by gender
                    if (gender != null && !gender.trim().isEmpty()) {
                        if (!referenceRanges.contains(gender.toLowerCase())) {
                            matches = false;
                        }
                    }

                    // Search by age minimum
                    if (ageMin != null && !ageMin.trim().isEmpty() && matches) {
                        if (!referenceRanges.contains(ageMin.toLowerCase())) {
                            matches = false;
                        }
                    }

                    // Search by age maximum
                    if (ageMax != null && !ageMax.trim().isEmpty() && matches) {
                        if (!referenceRanges.contains(ageMax.toLowerCase())) {
                            matches = false;
                        }
                    }

                    if (matches) {
                        filteredReferences.add(reference);
                    }
                }
            }

            return ApiResponseHelper.successResponse("Test references found by reference ranges", filteredReferences);
        } catch (Exception e) {
            LOGGER.error("Error searching test references by reference ranges: {}", e.getMessage(), e);
            return ApiResponseHelper.errorResponse("Error searching test references: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
