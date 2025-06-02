package tiameds.com.tiameds.services.lab;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
        List<SuperAdminReferanceEntity> testReferenceEntities = new ArrayList<>();
        if (currentUser == null) {
            throw new RuntimeException("User authentication failed.");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
//            Category, Test Name, Test Description, Units, Gender, Min Reference Range, Max Reference Range, Age Min, Age Max
            for (CSVRecord record : csvParser) {
                try {
                    SuperAdminReferanceEntity entity = new SuperAdminReferanceEntity();
                    entity.setCategory(record.get("Category").trim());
                    entity.setTestName(record.get("Test Name").trim());
                    entity.setTestDescription(record.get("Test Description").trim());
                    entity.setUnits(record.get("Units").trim().isEmpty() ? null : record.get("Units").trim());

                    // Gender Validation
                    String genderStr = record.get("Gender").trim().toUpperCase();
                    try {
                        entity.setGender(Gender.valueOf(genderStr));
                    } catch (IllegalArgumentException e) {
                        LOGGER.wait(Long.parseLong("Skipping record due to invalid"));

                    }
                    // Convert Min and Max safely
                    try {
                        entity.setMinReferenceRange(Double.parseDouble(record.get("Min Reference Range").trim()));
                        entity.setMaxReferenceRange(Double.parseDouble(record.get("Max Reference Range").trim()));
                    } catch (NumberFormatException e) {
                        LOGGER.wait(Long.parseLong("Skipping record due to invalid number format"));
                        continue;
                    }
                    // Set Age Range
                    try {
                        entity.setAgeMin(Integer.parseInt(record.get("Age Min").trim()));
                        entity.setAgeMax(Integer.parseInt(record.get("Age Max").trim()));
                    } catch (NumberFormatException e) {
                        entity.setAgeMin(0);
                        entity.setAgeMax(100);
                    }
                    entity.setCreatedBy(currentUser.getUsername());
                    entity.setUpdatedBy(currentUser.getUsername());
                    entity.setCreatedAt(LocalDateTime.now());
                    entity.setUpdatedAt(LocalDateTime.now());
                    // Save the entity
                    testReferenceEntities.add(superAdminReferanceRepository.save(entity));
                } catch (Exception ex) {
                    LOGGER.wait(Long.parseLong("Skipping row due to error: " + ex.getMessage()));
                }
            }
            return testReferenceEntities;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process CSV file: " + e.getMessage());
        }
    }


    public ResponseEntity<?> downloadTestReferanceCSV() {
        List<SuperAdminReferanceEntity>  testReferences = superAdminReferanceRepository.findAll();
        // Generate CSV content
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Category,Test Name,Test Description,Units,Gender,Min,Max,Age Min,Age Max\n");

        for (SuperAdminReferanceEntity testReference : testReferences) {
            // Ensure proper formatting by escaping commas in values
            csvContent.append("\"").append(testReference.getCategory().replace("\"", "\"\"")).append("\",");
            csvContent.append("\"").append(testReference.getTestName().replace("\"", "\"\"")).append("\",");
            csvContent.append("\"").append(testReference.getTestDescription().replace("\"", "\"\"")).append("\",");
            csvContent.append("\"").append(testReference.getUnits() != null ? testReference.getUnits().replace("\"", "\"\"") : "").append("\",");
            csvContent.append("\"").append(testReference.getGender()).append("\",");
            csvContent.append(testReference.getMinReferenceRange()).append(",");
            csvContent.append(testReference.getMaxReferenceRange()).append(",");
            csvContent.append(testReference.getAgeMin()).append(",");
            csvContent.append(testReference.getAgeMax()).append("\n");
        }
        // Set the response headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + "_test_references.csv");
        headers.add("Content-Type", "text/csv; charset=UTF-8");

        // Return the CSV content as a ResponseEntity
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent.toString());

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
                    dto.setAgeMax(testReference.getAgeMax());
                    dto.setCreatedAt(testReference.getCreatedAt());
                    dto.setUpdatedAt(testReference.getUpdatedAt());
                    return dto;
                })
                .sorted(Comparator.comparing(TestReferenceDTO::getCategory).thenComparing(TestReferenceDTO::getTestName))
                .collect(Collectors.toList());

        return ApiResponseHelper.successResponse("Test references retrieved successfully", testReferenceDTOs);
    }
}
