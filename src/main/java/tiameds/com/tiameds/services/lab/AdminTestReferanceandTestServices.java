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
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

//    @Transactional
//    public List<SuperAdminReferanceEntity> uploadTestReferance(MultipartFile file, User currentUser) {
//        List<SuperAdminReferanceEntity> testReferenceEntities = new ArrayList<>();
//        if (currentUser == null) {
//            throw new RuntimeException("User authentication failed.");
//        }
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
//             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

    /// /            Category, Test Name, Test Description, Units, Gender, Min Reference Range, Max Reference Range, Age Min, Age Max
//            for (CSVRecord record : csvParser) {
//                try {
//                    SuperAdminReferanceEntity entity = new SuperAdminReferanceEntity();
//                    entity.setCategory(record.get("Category").trim());
//                    entity.setTestName(record.get("Test Name").trim());
//                    entity.setTestDescription(record.get("Test Description").trim());
//                    entity.setUnits(record.get("Units").trim().isEmpty() ? null : record.get("Units").trim());
//
//                    // Gender Validation
//                    String genderStr = record.get("Gender").trim().toUpperCase();
//                    try {
//                        entity.setGender(Gender.valueOf(genderStr));
//                    } catch (IllegalArgumentException e) {
//                        LOGGER.wait(Long.parseLong("Skipping record due to invalid"));
//
//                    }
//                    // Convert Min and Max safely
//                    try {
//                        entity.setMinReferenceRange(Double.parseDouble(record.get("Min Reference Range").trim()));
//                        entity.setMaxReferenceRange(Double.parseDouble(record.get("Max Reference Range").trim()));
//                    } catch (NumberFormatException e) {
//                        LOGGER.wait(Long.parseLong("Skipping record due to invalid number format"));
//                        continue;
//                    }
//                    // Set Age Range
//                    try {
//                        entity.setAgeMin(Integer.parseInt(record.get("Age Min").trim()));
//                        entity.setAgeMax(Integer.parseInt(record.get("Age Max").trim()));
//                    } catch (NumberFormatException e) {
//                        entity.setAgeMin(0);
//                        entity.setAgeMax(100);
//                    }
//                    entity.setCreatedBy(currentUser.getUsername());
//                    entity.setUpdatedBy(currentUser.getUsername());
//                    entity.setCreatedAt(LocalDateTime.now());
//                    entity.setUpdatedAt(LocalDateTime.now());
//                    // Save the entity
//                    testReferenceEntities.add(superAdminReferanceRepository.save(entity));
//                } catch (Exception ex) {
//                    LOGGER.wait(Long.parseLong("Skipping row due to error: " + ex.getMessage()));
//                }
//            }
//            return testReferenceEntities;
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to process CSV file: " + e.getMessage());
//        }
//    }


//    @Transactional
//    public List<SuperAdminReferanceEntity> uploadTestReferance(MultipartFile file, User currentUser) {
//        List<SuperAdminReferanceEntity> testReferenceEntities = new ArrayList<>();
//        if (currentUser == null) {
//            throw new RuntimeException("User authentication failed.");
//        }
//
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
//             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase())) {
//            for (CSVRecord record : csvParser) {
//                try {
//                    String category = getStringOrBlank(record, "Category");
//                    String testName = getStringOrBlank(record, "Test Name");
//
//                    if (category.isEmpty() || testName.isEmpty()) {
//                        throw new IllegalArgumentException("Category and Test Name are required fields");
//                    }
//
//                    SuperAdminReferanceEntity entity = new SuperAdminReferanceEntity();
//                    entity.setCategory(category);
//                    entity.setTestName(testName);
//                    entity.setTestDescription(getStringOrBlank(record, "Test Description"));
//                    entity.setUnits(getStringOrBlank(record, "Units"));
//
//                    String genderStr = getStringOrBlank(record, "Gender");
//                    if (!genderStr.isEmpty()) {
//                        try {
//                            entity.setGender(Gender.valueOf(genderStr.trim().toUpperCase()));
//                        } catch (IllegalArgumentException e) {
//                            LOGGER.warn("Invalid gender value '{}' at record {} — skipping gender", genderStr, record.getRecordNumber());
//                        }
//                    }
//                    // Numeric values
//                    entity.setMinReferenceRange(parseDoubleOrBlank(record, "Min Reference Range"));
//                    entity.setMaxReferenceRange(parseDoubleOrBlank(record, "Max Reference Range"));
//
//                    // Age
//                    entity.setAgeMin(parseIntWithDefault(record, "Age Min", 0));
//                    entity.setAgeMax(parseIntWithDefault(record, "Age Max", 100));
//
//                    // Audit info
//                    entity.setCreatedBy(currentUser.getUsername());
//                    entity.setUpdatedBy(currentUser.getUsername());
//                    entity.setCreatedAt(LocalDateTime.now());
//                    entity.setUpdatedAt(LocalDateTime.now());
//
//                    testReferenceEntities.add(superAdminReferanceRepository.save(entity));
//                } catch (Exception ex) {
//                    LOGGER.error("Error in record {}: {}", record.getRecordNumber(), ex.getMessage());
//                    throw new RuntimeException("Error in record " + record.getRecordNumber() + ": " + ex.getMessage(), ex);
//                }
//            }
//            return testReferenceEntities;
//
//        } catch (Exception e) {
//            LOGGER.error("Failed to process CSV file: {}", e.getMessage(), e);
//            throw new RuntimeException("Failed to process CSV file: " + e.getMessage(), e);
//        }
//    }
//
//    private String getStringOrBlank(CSVRecord record, String column) {
//        try {
//            String value = record.get(column);
//            return value == null ? "" : value.trim();
//        } catch (IllegalArgumentException e) {
//            return "";
//        }
//    }
//
//    private Double parseDoubleOrBlank(CSVRecord record, String column) {
//        try {
//            String value = getStringOrBlank(record, column);
//            return value.isEmpty() ? 0 : Double.parseDouble(value);
//        } catch (NumberFormatException e) {
//            LOGGER.warn("Invalid number in column '{}': {}", column, e.getMessage());
//            return null;
//        }
//    }
//
//    private Integer parseIntWithDefault(CSVRecord record, String column, Integer defaultValue) {
//        try {
//            String value = getStringOrBlank(record, column);
//            return value.isEmpty() ? defaultValue : Integer.parseInt(value);
//        } catch (NumberFormatException e) {
//            LOGGER.warn("Invalid integer in column '{}': {}. Using default: {}", column, e.getMessage(), defaultValue);
//            return defaultValue;
//        }
//    }
//
    @Transactional
    public List<SuperAdminReferanceEntity> uploadTestReferance(MultipartFile file, User currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("User authentication failed.");
        }

        List<SuperAdminReferanceEntity> testReferenceEntities = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord record : csvParser) {
                SuperAdminReferanceEntity entity = processRecord(record, currentUser);
                testReferenceEntities.add(superAdminReferanceRepository.save(entity));
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

        // Audit fields
        entity.setCreatedBy(currentUser.getUsername());
        entity.setUpdatedBy(currentUser.getUsername());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        return entity;
    }

    // Helper Methods
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
            try {
                entity.setGender(Gender.valueOf(genderStr.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid gender '{}' (record {}). Skipping.",
                        genderStr, record.getRecordNumber());
            }
        }
    }
//
//    public ResponseEntity<?> downloadTestReferanceCSV() {
//        List<SuperAdminReferanceEntity> testReferences = superAdminReferanceRepository.findAll();
//        // Generate CSV content
//        StringBuilder csvContent = new StringBuilder();

    /// /        csvContent.append("Category,Test Name,Test Description,Units,Gender,Min,Max,Age Min,Age Max\n");
//        csvContent.append("Category,Test Name,Test Description,Units,Gender,Min,Max,Age Min,Min Age Unit,Age Max,Max Age Unit\n");
//
//        for (SuperAdminReferanceEntity testReference : testReferences) {
//            // Ensure proper formatting by escaping commas in values
//            csvContent.append("\"").append(testReference.getCategory().replace("\"", "\"\"")).append("\",");
//            csvContent.append("\"").append(testReference.getTestName().replace("\"", "\"\"")).append("\",");
//            csvContent.append("\"").append(testReference.getTestDescription().replace("\"", "\"\"")).append("\",");
//            csvContent.append("\"").append(testReference.getUnits() != null ? testReference.getUnits().replace("\"", "\"\"") : "").append("\",");
//            csvContent.append("\"").append(testReference.getGender()).append("\",");
//            csvContent.append(testReference.getMinReferenceRange()).append(",");
//            csvContent.append(testReference.getMaxReferenceRange()).append(",");
//            csvContent.append(testReference.getAgeMin()).append(",");
//            csvContent.append(testReference.getMinAgeUnit()).append(",");
//            csvContent.append(testReference.getAgeMax()).append("\n");
//            csvContent.append(testReference.getMaxAgeUnit()).append(",");
//        }
//        // Set the response headers for file download
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Content-Disposition", "attachment; filename=" + "_test_references.csv");
//        headers.add("Content-Type", "text/csv; charset=UTF-8");
//
//        // Return the CSV content as a ResponseEntity
//        return ResponseEntity.ok()
//                .headers(headers)
//                .body(csvContent.toString());
//
//    }
    public ResponseEntity<?> downloadTestReferanceCSV() {
        List<SuperAdminReferanceEntity> testReferences = superAdminReferanceRepository.findAll();

        // Generate CSV content
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Category,Test Name,Test Description,Units,Gender,Min,Max,Age Min,Min Age Unit,Age Max,Max Age Unit\n");

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
            csvContent.append("\"").append(escapeCsv(String.valueOf(testReference.getMaxAgeUnit()))).append("\"\n");
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
}
