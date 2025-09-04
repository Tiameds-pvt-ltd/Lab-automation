package tiameds.com.tiameds.services.lab;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.dto.lab.TestReferenceDTO;
import tiameds.com.tiameds.entity.*;
import tiameds.com.tiameds.repository.TestReferenceRepository;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;



@Slf4j
@Service
public class TestReferenceServices {

    private final TestReferenceRepository testReferenceRepository;
    private static final Logger LOGGER = Logger.getLogger(TestReferenceServices.class.getName());


    public TestReferenceServices(TestReferenceRepository testReferenceRepository) {
        this.testReferenceRepository = testReferenceRepository;
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
                    dto.setMinAgeUnit(TestReferenceEntity.getMinAgeUnit() != null ? TestReferenceEntity.getMinAgeUnit().toString() : null);
                    dto.setAgeMax(TestReferenceEntity.getAgeMax());
                    dto.setMaxAgeUnit(TestReferenceEntity.getMaxAgeUnit() != null ? TestReferenceEntity.getMaxAgeUnit().toString() : null);
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
        testReferenceEntity.setMinAgeUnit(AgeUnit.valueOf(testReferenceDTO.getMinAgeUnit()));
        testReferenceEntity.setAgeMax(testReferenceDTO.getAgeMax());
        testReferenceEntity.setMaxAgeUnit(AgeUnit.valueOf(testReferenceDTO.getMaxAgeUnit()));
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
        entity.setMinAgeUnit(AgeUnit.valueOf(testReferenceDTO.getMinAgeUnit()));
        entity.setAgeMax(testReferenceDTO.getAgeMax());
        entity.setMaxAgeUnit(AgeUnit.valueOf(testReferenceDTO.getMaxAgeUnit()));
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
        csvContent.append("Category,Test Name,Test Description,Units,Gender,Min Reference Range,Max Reference Range,Min,Min Age Unit,Age Max,Max Age Unit,Created By,Updated By,Created At,Updated At\n");

        for (TestReferenceEntity entity : testReferenceEntities) {
            csvContent.append("\"").append(escapeCSV(entity.getCategory())).append("\",");
            csvContent.append("\"").append(escapeCSV(entity.getTestName())).append("\",");
            csvContent.append("\"").append(escapeCSV(entity.getTestDescription())).append("\",");
            csvContent.append("\"").append(escapeCSV(entity.getUnits())).append("\",");
            csvContent.append("\"").append(entity.getGender() != null ? entity.getGender().toString() : "").append("\",");
            csvContent.append("\"").append(entity.getMinReferenceRange() != null ? entity.getMinReferenceRange().toString() : "").append("\",");
            csvContent.append("\"").append(entity.getMaxReferenceRange() != null ? entity.getMaxReferenceRange().toString() : "").append("\",");
            csvContent.append("\"").append(entity.getAgeMin() != null ? entity.getAgeMin().toString() : "").append("\",");
            csvContent.append("\"").append(entity.getMinAgeUnit() != null ? entity.getMinAgeUnit().toString() : "").append("\",");
            csvContent.append("\"").append(entity.getAgeMax() != null ? entity.getAgeMax().toString() : "").append("\",");
            csvContent.append("\"").append(entity.getMaxAgeUnit() != null ? entity.getMaxAgeUnit().toString() : "").append("\",");
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


    public List<TestReferenceDTO>getTestReferenceByTestName(Lab lab, String testName) {
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
                    dto.setMinAgeUnit(TestReferenceEntity.getMinAgeUnit() != null ? TestReferenceEntity.getMinAgeUnit().toString() : null);
                    dto.setAgeMax(TestReferenceEntity.getAgeMax());
                    dto.setMaxAgeUnit(TestReferenceEntity.getMaxAgeUnit() != null ? TestReferenceEntity.getMaxAgeUnit().toString() : null);
                    dto.setCreatedBy(TestReferenceEntity.getCreatedBy());
                    dto.setUpdatedBy(TestReferenceEntity.getUpdatedBy());
                    dto.setCreatedAt(TestReferenceEntity.getCreatedAt());
                    dto.setUpdatedAt(TestReferenceEntity.getUpdatedAt());
                    return dto;
                }).toList();
        return testReferenceDTOS;
    }

    public void deleteAllTestReferences(Lab lab) {
        List<TestReferenceEntity> testReferences = new ArrayList<>(lab.getTestReferences());
        for (TestReferenceEntity testReference : testReferences) {
            lab.removeTestReference(testReference);
            testReferenceRepository.delete(testReference);
        }
        LOGGER.info("All test references deleted successfully.");
    }

    //============================================ updating referance and  fix meau and beta Symbol========================//
    @Transactional
    public List<TestReferenceEntity> uploadCsv(Lab lab, MultipartFile file, User currentUser) {

        if (currentUser == null) {
            throw new RuntimeException("User authentication failed.");
        }

        List<TestReferenceEntity> testReferenceEntities = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            for (CSVRecord record : csvParser) {
                try {
//                    TestReferenceEntity entity = processRecord(record, currentUser, lab); // ✅ pass lab
//                    testReferenceEntities.add(entity);
                    TestReferenceEntity entity = processRecord(record, currentUser, lab);
                    // ✅ save one by one in order
                    TestReferenceEntity saved = testReferenceRepository.save(entity);
                    testReferenceEntities.add(saved);
                } catch (Exception ex) {
                    LOGGER.warning("Skipping row " + record.getRecordNumber() + " due to error: " + ex.getMessage());
                    // Continue processing other rows instead of failing completely
                }
            }

        } catch (Exception ex) {
            LOGGER.warning("Failed to process CSV file: " + ex.getMessage());
            throw new RuntimeException("Failed to process CSV file: " + ex.getMessage(), ex);
        }

        // Save all valid entities
        if (!testReferenceEntities.isEmpty()) {
            try {
                return testReferenceRepository.saveAll(testReferenceEntities); // ✅ no need to add manually to lab
            } catch (Exception e) {
                LOGGER.warning("Failed to save entities due to constraint violation: " + e.getMessage());
                throw new RuntimeException("Failed to save entities due to database constraints: " + e.getMessage(), e);
            }
        }

        return testReferenceEntities;
    }

    private TestReferenceEntity processRecord(CSVRecord record, User currentUser, Lab lab) {
        // Required fields validation
        String category = getStringOrBlank(record, "Category");
        String testName = getStringOrBlank(record, "Test Name");
        if (category.isEmpty() || testName.isEmpty()) {
            throw new IllegalArgumentException("Record #" + record.getRecordNumber() + ": Category and Test Name are required");
        }

        TestReferenceEntity entity = new TestReferenceEntity();
        entity.setCategory(category);
        entity.setTestName(testName);
        entity.setTestDescription(getStringOrBlank(record, "Test Description"));
        entity.setUnits(getStringOrBlank(record, "Units"));

        // Handle Gender
        parseGender(record, entity);

        // Reference ranges (nullable)
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

        // ✅ set relationship properly
        lab.addTestReference(entity);

        return entity;
    }

    private void parseGender(CSVRecord record, TestReferenceEntity entity) {
        String genderStr = getStringOrBlank(record, "Gender");
        if (!genderStr.isEmpty()) {
            genderStr = genderStr.trim().toUpperCase();
            if (genderStr.equals("M/F")) {
                genderStr = "MF"; // normalize
            }
        } else {
            genderStr = "MF"; // default when empty
        }

        switch (genderStr) {
            case "M":
                entity.setGender(Gender.M);
                break;
            case "F":
                entity.setGender(Gender.F);
                break;
            case "MF":
                entity.setGender(Gender.MF);
                break;
            default:
                LOGGER.warning("Unknown gender '" + genderStr + "' (record " + record.getRecordNumber() + "). Defaulting to MF.");
                entity.setGender(Gender.MF);
        }
    }

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
            LOGGER.warning("Invalid number in column '" + column + "' (record " + record.getRecordNumber() + "): " + e.getMessage());
            return null;
        }
    }

    private Integer parseIntWithDefault(CSVRecord record, String column, int defaultValue) {
        String value = getStringOrBlank(record, column);
        if (value.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid integer in column '" + column + "' (record " + record.getRecordNumber() + "). Using default: " + defaultValue);
            return defaultValue;
        }
    }

    private AgeUnit parseAgeUnitWithDefault(CSVRecord record, String column) {
        String value = getStringOrBlank(record, column);
        if (value.isEmpty()) return AgeUnit.YEARS; // Default unit
        try {
            return AgeUnit.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid age unit '" + value + "' (record " + record.getRecordNumber() + "). Defaulting to YEARS.");
            return AgeUnit.YEARS;
        }
    }


}
