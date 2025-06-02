package tiameds.com.tiameds.services.lab;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.dto.lab.TestReferenceDTO;
import tiameds.com.tiameds.entity.*;
import tiameds.com.tiameds.repository.TestReferenceRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

@Service
public class TestReferenceServices {

    private final TestReferenceRepository testReferenceRepository;
    private static final Logger LOGGER = Logger.getLogger(TestReferenceServices.class.getName());

    public TestReferenceServices(TestReferenceRepository testReferenceRepository) {
        this.testReferenceRepository = testReferenceRepository;
    }

    @Transactional
    public List<TestReferenceEntity> uploadCsv(Lab lab, MultipartFile file, User currentUser) {
        List<TestReferenceEntity> testReferenceEntities = new ArrayList<>();

        if (currentUser == null) {
            throw new RuntimeException("User authentication failed.");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                try {
                    TestReferenceEntity entity = new TestReferenceEntity();
                    entity.setCategory(record.get("Category").trim());
                    entity.setTestName(record.get("Test Name").trim());
                    entity.setTestDescription(record.get("Test Description").trim());
                    entity.setUnits(record.get("Units").trim().isEmpty() ? null : record.get("Units").trim());

                    // Gender Validation
                    String genderStr = record.get("Gender").trim().toUpperCase();
                    try {
                        entity.setGender(Gender.valueOf(genderStr));
                    } catch (IllegalArgumentException e) {
                        LOGGER.warning("Skipping record due to invalid gender: " + genderStr);
                        continue;
                    }

                    // Convert Min and Max safely
                    try {
                        entity.setMinReferenceRange(Double.parseDouble(record.get("Min")));
                        entity.setMaxReferenceRange(Double.parseDouble(record.get("Max")));
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Skipping record due to invalid Min/Max values.");
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

                    testReferenceEntities.add(entity);
                    lab.addTestReference(entity);


                } catch (Exception ex) {
                    LOGGER.warning("Skipping row due to error: " + ex.getMessage());
                }
            }
            return testReferenceRepository.saveAll(testReferenceEntities);

        } catch (Exception e) {
            throw new RuntimeException("Failed to process CSV file: " + e.getMessage());
        }
    }

    public List<TestReferenceDTO> getAllTestReferences(Lab lab) {
        List<TestReferenceDTO> testReferenceDTOS = lab.getTestReferences().stream()
                .sorted(Comparator.comparingLong(TestReferenceEntity::getId))
                .map(TestReferenceEntity -> {
                    TestReferenceDTO dto = new TestReferenceDTO();
                    dto.setId(TestReferenceEntity.getId());
                    dto.setCategory(TestReferenceEntity.getCategory());
                    dto.setTestName(TestReferenceEntity.getTestName());
                    dto.setTestDescription(TestReferenceEntity.getTestDescription());
                    dto.setUnits(TestReferenceEntity.getUnits());
                    dto.setGender(TestReferenceEntity.getGender());
                    dto.setMinReferenceRange(TestReferenceEntity.getMinReferenceRange());
                    dto.setMaxReferenceRange(TestReferenceEntity.getMaxReferenceRange());
                    dto.setAgeMin(TestReferenceEntity.getAgeMin());
                    dto.setAgeMax(TestReferenceEntity.getAgeMax());
                    dto.setCreatedBy(TestReferenceEntity.getCreatedBy());
                    dto.setUpdatedBy(TestReferenceEntity.getUpdatedBy());
                    dto.setCreatedAt(TestReferenceEntity.getCreatedAt());
                    dto.setUpdatedAt(TestReferenceEntity.getUpdatedAt());
                    return dto;
                }).toList();
        return testReferenceDTOS;

    }

    public TestReferenceDTO updateTestReference(Lab lab, Long testReferenceId, TestReferenceDTO testReferenceDTO, User currentUser) {
        TestReferenceEntity testReferenceEntity = lab.getTestReferences().stream()
                .filter(entity -> entity.getId().equals(testReferenceId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test reference not found."));

        testReferenceEntity.setCategory(testReferenceDTO.getCategory());
        testReferenceEntity.setTestName(testReferenceDTO.getTestName());
        testReferenceEntity.setTestDescription(testReferenceDTO.getTestDescription());
        testReferenceEntity.setUnits(testReferenceDTO.getUnits());
        testReferenceEntity.setGender(testReferenceDTO.getGender());
        testReferenceEntity.setMinReferenceRange(testReferenceDTO.getMinReferenceRange());
        testReferenceEntity.setMaxReferenceRange(testReferenceDTO.getMaxReferenceRange());
        testReferenceEntity.setAgeMin(testReferenceDTO.getAgeMin());
        testReferenceEntity.setAgeMax(testReferenceDTO.getAgeMax());
        testReferenceEntity.setUpdatedBy(currentUser.getUsername());

        testReferenceRepository.save(testReferenceEntity);

        TestReferenceDTO dto = new TestReferenceDTO();
        dto.setId(testReferenceEntity.getId());
        dto.setCategory(testReferenceEntity.getCategory());
        dto.setTestName(testReferenceEntity.getTestName());
        dto.setTestDescription(testReferenceEntity.getTestDescription());
        dto.setUnits(testReferenceEntity.getUnits());
        dto.setGender(testReferenceEntity.getGender());
        dto.setMinReferenceRange(testReferenceEntity.getMinReferenceRange());
        dto.setMaxReferenceRange(testReferenceEntity.getMaxReferenceRange());
        dto.setAgeMin(testReferenceEntity.getAgeMin());
        dto.setAgeMax(testReferenceEntity.getAgeMax());
        dto.setCreatedBy(testReferenceEntity.getCreatedBy());
        dto.setUpdatedBy(testReferenceEntity.getUpdatedBy());
        dto.setCreatedAt(testReferenceEntity.getCreatedAt());
        dto.setUpdatedAt(testReferenceEntity.getUpdatedAt());

        return dto;
    }

    public void deleteTestReference(Lab lab, Long testReferenceId) {
        TestReferenceEntity testReferenceEntity = lab.getTestReferences().stream()
                .filter(entity -> entity.getId().equals(testReferenceId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Test reference not found."));

        lab.removeTestReference(testReferenceEntity);
        testReferenceRepository.delete(testReferenceEntity);
    }

    public TestReferenceDTO addTestReference(Lab lab, TestReferenceDTO testReferenceDTO, User currentUser) {
        TestReferenceEntity entity = new TestReferenceEntity();
        entity.setCategory(testReferenceDTO.getCategory());
        entity.setTestName(testReferenceDTO.getTestName());
        entity.setTestDescription(testReferenceDTO.getTestDescription());
        entity.setUnits(testReferenceDTO.getUnits());
        entity.setGender(testReferenceDTO.getGender());
        entity.setMinReferenceRange(testReferenceDTO.getMinReferenceRange());
        entity.setMaxReferenceRange(testReferenceDTO.getMaxReferenceRange());
        entity.setAgeMin(testReferenceDTO.getAgeMin());
        entity.setAgeMax(testReferenceDTO.getAgeMax());
        entity.setCreatedBy(currentUser.getUsername());
        entity.setUpdatedBy(currentUser.getUsername());

        testReferenceRepository.save(entity);
        lab.addTestReference(entity);

        TestReferenceDTO dto = new TestReferenceDTO();
        dto.setId(entity.getId());
        dto.setCategory(entity.getCategory());
        dto.setTestName(entity.getTestName());
        dto.setTestDescription(entity.getTestDescription());
        dto.setUnits(entity.getUnits());
        dto.setGender(entity.getGender());
        dto.setMinReferenceRange(entity.getMinReferenceRange());
        dto.setMaxReferenceRange(entity.getMaxReferenceRange());
        dto.setAgeMin(entity.getAgeMin());
        dto.setAgeMax(entity.getAgeMax());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    public ResponseEntity<?> downloadTestReference(Lab lab) {
        // Fetch all test references associated with the specified lab
        List<TestReferenceEntity> testReferenceEntities = new ArrayList<>(lab.getTestReferences());

        // Generate CSV content
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Category,Test Name,Test Description,Units,Gender,Min Reference Range,Max Reference Range,Age Min,Age Max,Created By,Updated By,Created At,Updated At\n");

        for (TestReferenceEntity entity : testReferenceEntities) {
            csvContent.append("\"").append(escapeCSV(entity.getCategory())).append("\",");
            csvContent.append("\"").append(escapeCSV(entity.getTestName())).append("\",");
            csvContent.append("\"").append(escapeCSV(entity.getTestDescription())).append("\",");
            csvContent.append("\"").append(escapeCSV(entity.getUnits())).append("\",");
            csvContent.append("\"").append(entity.getGender() != null ? entity.getGender().toString() : "").append("\",");
            csvContent.append("\"").append(entity.getMinReferenceRange() != null ? entity.getMinReferenceRange().toString() : "").append("\",");
            csvContent.append("\"").append(entity.getMaxReferenceRange() != null ? entity.getMaxReferenceRange().toString() : "").append("\",");
            csvContent.append("\"").append(entity.getAgeMin() != null ? entity.getAgeMin().toString() : "").append("\",");
            csvContent.append("\"").append(entity.getAgeMax() != null ? entity.getAgeMax().toString() : "").append("\",");
            csvContent.append("\"").append(escapeCSV(entity.getCreatedBy())).append("\",");
            csvContent.append("\"").append(escapeCSV(entity.getUpdatedBy())).append("\",");
            csvContent.append("\"").append(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : "").append("\",");
            csvContent.append("\"").append(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : "").append("\"\n");
        }

        // Set the response headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + lab.getName() + "_test_references.csv");
        headers.add("Content-Type", "text/csv; charset=UTF-8");

        // Return the CSV content as a ResponseEntity
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent.toString());
    }

    /**
     * Escapes double quotes and ensures proper CSV formatting.
     */
    private String escapeCSV(String value) {
        return value != null ? value.replace("\"", "\"\"") : "";
    }


    public List<TestReferenceDTO> getTestReferenceByTestName(Lab lab, String testName) {
        List<TestReferenceDTO> testReferenceDTOS = lab.getTestReferences().stream()
                .filter(testReferenceEntity -> testReferenceEntity.getTestName().equalsIgnoreCase(testName))
                .sorted(Comparator.comparingLong(TestReferenceEntity::getId))
                .map(TestReferenceEntity -> {
                    TestReferenceDTO dto = new TestReferenceDTO();
                    dto.setId(TestReferenceEntity.getId());
                    dto.setCategory(TestReferenceEntity.getCategory());
                    dto.setTestName(TestReferenceEntity.getTestName());
                    dto.setTestDescription(TestReferenceEntity.getTestDescription());
                    dto.setUnits(TestReferenceEntity.getUnits());
                    dto.setGender(TestReferenceEntity.getGender());
                    dto.setMinReferenceRange(TestReferenceEntity.getMinReferenceRange());
                    dto.setMaxReferenceRange(TestReferenceEntity.getMaxReferenceRange());
                    dto.setAgeMin(TestReferenceEntity.getAgeMin());
                    dto.setAgeMax(TestReferenceEntity.getAgeMax());
                    dto.setCreatedBy(TestReferenceEntity.getCreatedBy());
                    dto.setUpdatedBy(TestReferenceEntity.getUpdatedBy());
                    dto.setCreatedAt(TestReferenceEntity.getCreatedAt());
                    dto.setUpdatedAt(TestReferenceEntity.getUpdatedAt());
                    return dto;
                }).toList();
        return testReferenceDTOS;
    }
}
