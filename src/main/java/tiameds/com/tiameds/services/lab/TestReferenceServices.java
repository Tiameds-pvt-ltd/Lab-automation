package tiameds.com.tiameds.services.lab;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.dto.lab.TestReferenceDTO;
import tiameds.com.tiameds.entity.*;
import tiameds.com.tiameds.repository.LabTestReferenceLinkRepository;
import tiameds.com.tiameds.repository.TestReferenceRepository;
import tiameds.com.tiameds.utils.EncodingUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;



@Slf4j
@Service
public class TestReferenceServices {

    private final TestReferenceRepository testReferenceRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final LabTestReferenceLinkRepository labTestReferenceLinkRepository;
    private static final Logger LOGGER = Logger.getLogger(TestReferenceServices.class.getName());

    @PersistenceContext
    private EntityManager entityManager;

    public TestReferenceServices(TestReferenceRepository testReferenceRepository,
                                 SequenceGeneratorService sequenceGeneratorService,
                                 LabTestReferenceLinkRepository labTestReferenceLinkRepository) {
        this.testReferenceRepository = testReferenceRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.labTestReferenceLinkRepository = labTestReferenceLinkRepository;
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
                    
                    // Add JSON fields
                    dto.setReportJson(TestReferenceEntity.getReportJson());
                    dto.setReferenceRanges(TestReferenceEntity.getReferenceRanges());
                    
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
        
        // Add JSON fields
        testReferenceEntity.setReportJson(testReferenceDTO.getReportJson());
        testReferenceEntity.setReferenceRanges(testReferenceDTO.getReferenceRanges());
        
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
        
        // Add JSON fields to response DTO
        dto.setReportJson(testReferenceEntity.getReportJson());
        dto.setReferenceRanges(testReferenceEntity.getReferenceRanges());
        
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
        alignReferenceSequence(lab.getId());

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
        
        // Add JSON fields
        entity.setReportJson(testReferenceDTO.getReportJson());
        entity.setReferenceRanges(testReferenceDTO.getReferenceRanges());
        
        entity.setCreatedBy(currentUser.getUsername());
        entity.setUpdatedBy(currentUser.getUsername());
        entity.setTestReferenceCode(generateUniqueReferenceCode(lab.getId()));
        TestReferenceEntity saved = testReferenceRepository.save(entity);
        labTestReferenceLinkRepository.linkLabToReference(lab.getId(), saved.getId());

        TestReferenceDTO dto = new TestReferenceDTO();
        dto.setId(saved.getId());
        dto.setCategory(saved.getCategory());
        dto.setTestName(saved.getTestName());
        dto.setTestDescription(saved.getTestDescription());
        dto.setUnits(saved.getUnits());
        dto.setGender(saved.getGender());
        dto.setMinReferenceRange(saved.getMinReferenceRange());
        dto.setMaxReferenceRange(saved.getMaxReferenceRange());
        dto.setAgeMin(saved.getAgeMin());
        dto.setAgeMax(saved.getAgeMax());
        dto.setCreatedBy(saved.getCreatedBy());
        dto.setUpdatedBy(saved.getUpdatedBy());
        dto.setCreatedAt(saved.getCreatedAt());
        dto.setUpdatedAt(saved.getUpdatedAt());
        
        // Add JSON fields to response DTO
        dto.setReportJson(saved.getReportJson());
        dto.setReferenceRanges(saved.getReferenceRanges());
        
        return dto;
    }

    public ResponseEntity<?> downloadTestReference(Lab lab) {
        // Fetch all test references associated with the specified lab
        List<TestReferenceEntity> testReferenceEntities = new ArrayList<>(lab.getTestReferences());

        // Generate CSV content
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Category,Test Name,Test Description,Units,Gender,Min Reference Range,Max Reference Range,Age Min,Min Age Unit,Age Max,Max Age Unit,Remarks,ReportJson,ReferenceRanges,Created By,Updated By,Created At,Updated At\n");

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
            csvContent.append("\"").append("").append("\","); // Remarks column (empty for now)
            csvContent.append("\"").append(escapeCSV(entity.getReportJson() != null ? entity.getReportJson() : "")).append("\",");
            csvContent.append("\"").append(escapeCSV(entity.getReferenceRanges() != null ? entity.getReferenceRanges() : "")).append("\",");
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
        if (testName == null || testName.trim().isEmpty()) {
            LOGGER.warning("Test name is null or empty");
            return new ArrayList<>();
        }
        // Normalize the search term: trim, replace multiple spaces with single space, and normalize parentheses spacing
        String normalizedSearchTerm = testName.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("\\s*\\(", " (")
                .replaceAll("\\s*\\)", ") ");
        LOGGER.info("Normalized search term: '" + normalizedSearchTerm + "' (original: '" + testName + "')");
        LOGGER.info("Total test references in lab: " + lab.getTestReferences().size());
        
        // Log all test names in the lab for debugging
        if (LOGGER.isLoggable(java.util.logging.Level.INFO)) {
            List<String> allTestNames = lab.getTestReferences().stream()
                    .map(TestReferenceEntity::getTestName)
                    .filter(name -> name != null)
                    .toList();
            LOGGER.info("All test names in lab: " + String.join(", ", allTestNames));
        }
        
        List<TestReferenceDTO> testReferenceDTOS = lab.getTestReferences().stream()
                .filter(testReferenceEntity -> {
                    if (testReferenceEntity.getTestName() == null) {
                        return false;
                    }
                    // Normalize database value: trim, replace multiple spaces, and normalize parentheses spacing
                    String normalizedDbValue = testReferenceEntity.getTestName().trim()
                            .replaceAll("\\s+", " ")
                            .replaceAll("\\s*\\(", " (")
                            .replaceAll("\\s*\\)", ") ");
                    
                    // Try exact match first (case-insensitive)
                    boolean exactMatch = normalizedDbValue.equalsIgnoreCase(normalizedSearchTerm);
                    
                    // If no exact match, try contains match (case-insensitive)
                    // Only check if DB value contains the search term (not reverse, to avoid false positives)
                    boolean containsMatch = false;
                    if (!exactMatch && normalizedSearchTerm.length() >= 5) {
                        // Check if DB value contains the search term
                        containsMatch = normalizedDbValue.toLowerCase().contains(normalizedSearchTerm.toLowerCase());
                    }
                    
                    boolean matches = exactMatch || containsMatch;
                    
                    if (matches) {
                        LOGGER.info("Match found: DB='" + testReferenceEntity.getTestName() + "' normalized='" + normalizedDbValue + "' search='" + normalizedSearchTerm + "'");
                    }
                    
                    return matches;
                })
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
                    
                    // Add JSON fields
                    dto.setReportJson(TestReferenceEntity.getReportJson());
                    dto.setReferenceRanges(TestReferenceEntity.getReferenceRanges());
                    
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
//    @Transactional
//    public List<TestReferenceEntity> uploadCsv(Lab lab, MultipartFile file, User currentUser) {
//
//        if (currentUser == null) {
//            throw new RuntimeException("User authentication failed.");
//        }
//
//        List<TestReferenceEntity> testReferenceEntities = new ArrayList<>();
//
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
//             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
//                     .withFirstRecordAsHeader()
//                     .withIgnoreHeaderCase()
//                     .withTrim())) {
//
//            for (CSVRecord record : csvParser) {
//                try {
//                    TestReferenceEntity entity = processRecord(record, currentUser, lab); // ✅ pass lab
//                    testReferenceEntities.add(entity);
////                    TestReferenceEntity entity = processRecord(record, currentUser, lab);
////                    // ✅ save one by one in order
////                    TestReferenceEntity saved = testReferenceRepository.save(entity);
////                    testReferenceEntities.add(saved);
//                } catch (Exception ex) {
//                    LOGGER.warning("Skipping row " + record.getRecordNumber() + " due to error: " + ex.getMessage());
//                    // Continue processing other rows instead of failing completely
//                }
//            }
//
//        } catch (Exception ex) {
//            LOGGER.warning("Failed to process CSV file: " + ex.getMessage());
//            throw new RuntimeException("Failed to process CSV file: " + ex.getMessage(), ex);
//        }
//
//        // Save all valid entities
//        if (!testReferenceEntities.isEmpty()) {
//            try {
//                return testReferenceRepository.saveAll(testReferenceEntities); // ✅ no need to add manually to lab
//            } catch (Exception e) {
//                LOGGER.warning("Failed to save entities due to constraint violation: " + e.getMessage());
//                throw new RuntimeException("Failed to save entities due to database constraints: " + e.getMessage(), e);
//            }
//        }
//
//        return testReferenceEntities;
//    }
//
//    private TestReferenceEntity processRecord(CSVRecord record, User currentUser, Lab lab) {
//        // Required fields validation
//        String category = getStringOrBlank(record, "Category");
//        String testName = getStringOrBlank(record, "Test Name");
//        if (category.isEmpty() || testName.isEmpty()) {
//            throw new IllegalArgumentException("Record #" + record.getRecordNumber() + ": Category and Test Name are required");
//        }
//
//        TestReferenceEntity entity = new TestReferenceEntity();
//        entity.setCategory(category);
//        entity.setTestName(testName);
//        entity.setTestDescription(getStringOrBlank(record, "Test Description"));
//        entity.setUnits(getStringOrBlank(record, "Units"));
//
//        // Handle Gender
//        parseGender(record, entity);
//
//        // Reference ranges (nullable)
//        entity.setMinReferenceRange(parseDoubleOrNull(record, "Min Reference Range"));
//        entity.setMaxReferenceRange(parseDoubleOrNull(record, "Max Reference Range"));
//
//        // Age handling with DEFAULTS
//        entity.setAgeMin(parseIntWithDefault(record, "Age Min", 0));          // Default: 0
//        entity.setAgeMax(parseIntWithDefault(record, "Age Max", 100));       // Default: 100
//        entity.setMinAgeUnit(parseAgeUnitWithDefault(record, "Min Age Unit")); // Default: YEARS
//        entity.setMaxAgeUnit(parseAgeUnitWithDefault(record, "Max Age Unit")); // Default: YEARS
//
//        // Audit fields
//        entity.setCreatedBy(currentUser.getUsername());
//        entity.setUpdatedBy(currentUser.getUsername());
//        entity.setCreatedAt(LocalDateTime.now());
//        entity.setUpdatedAt(LocalDateTime.now());
//
//        // ✅ set relationship properly
//        lab.addTestReference(entity);
//
//        return entity;
//    }
//
//    private void parseGender(CSVRecord record, TestReferenceEntity entity) {
//        String genderStr = getStringOrBlank(record, "Gender");
//        if (!genderStr.isEmpty()) {
//            genderStr = genderStr.trim().toUpperCase();
//            if (genderStr.equals("M/F")) {
//                genderStr = "MF"; // normalize
//            }
//        } else {
//            genderStr = "MF"; // default when empty
//        }
//
//        switch (genderStr) {
//            case "M":
//                entity.setGender(Gender.M);
//                break;
//            case "F":
//                entity.setGender(Gender.F);
//                break;
//            case "MF":
//                entity.setGender(Gender.MF);
//                break;
//            default:
//                LOGGER.warning("Unknown gender '" + genderStr + "' (record " + record.getRecordNumber() + "). Defaulting to MF.");
//                entity.setGender(Gender.MF);
//        }
//    }
//
//    private String getStringOrBlank(CSVRecord record, String column) {
//        try {
//            String value = record.get(column);
//            return (value == null) ? "" : value.trim();
//        } catch (IllegalArgumentException e) {
//            return "";
//        }
//    }
//
//    private Double parseDoubleOrNull(CSVRecord record, String column) {
//        String value = getStringOrBlank(record, column);
//        if (value.isEmpty()) return null;
//        try {
//            return Double.parseDouble(value);
//        } catch (NumberFormatException e) {
//            LOGGER.warning("Invalid number in column '" + column + "' (record " + record.getRecordNumber() + "): " + e.getMessage());
//            return null;
//        }
//    }
//
//    private Integer parseIntWithDefault(CSVRecord record, String column, int defaultValue) {
//        String value = getStringOrBlank(record, column);
//        if (value.isEmpty()) return defaultValue;
//        try {
//            return Integer.parseInt(value);
//        } catch (NumberFormatException e) {
//            LOGGER.warning("Invalid integer in column '" + column + "' (record " + record.getRecordNumber() + "). Using default: " + defaultValue);
//            return defaultValue;
//        }
//    }
//
//    private AgeUnit parseAgeUnitWithDefault(CSVRecord record, String column) {
//        String value = getStringOrBlank(record, column);
//        if (value.isEmpty()) return AgeUnit.YEARS; // Default unit
//        try {
//            return AgeUnit.valueOf(value.trim().toUpperCase());
//        } catch (IllegalArgumentException e) {
//            LOGGER.warning("Invalid age unit '" + value + "' (record " + record.getRecordNumber() + "). Defaulting to YEARS.");
//            return AgeUnit.YEARS;
//        }
//    }


    @Transactional
    public List<TestReferenceEntity> uploadCsv(Lab lab, MultipartFile file, User currentUser) {

        if (currentUser == null) {
            throw new RuntimeException("User authentication failed.");
        }

        alignReferenceSequence(lab.getId());
        List<TestReferenceEntity> savedEntities = new ArrayList<>();

        try (BufferedReader reader = createNormalizedReader(file)) {
            try (CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim())) {

                for (CSVRecord record : csvParser) {
                    boolean persisted = false;
                    int attempts = 0;

                    while (!persisted && attempts < 5) {
                        attempts++;
                        TestReferenceEntity entity = null;
                        try {
                            entity = processRecord(record, currentUser);
                            entity.setTestReferenceCode(generateUniqueReferenceCode(lab.getId()));
                            TestReferenceEntity saved = testReferenceRepository.save(entity);
                            labTestReferenceLinkRepository.linkLabToReference(lab.getId(), saved.getId());
                            savedEntities.add(saved);
                            persisted = true;
                        } catch (DataIntegrityViolationException dive) {
                            handleReferenceDuplicate(lab, entity, dive);
                        } catch (Exception ex) {
                            LOGGER.warning("Skipping row " + record.getRecordNumber() +
                                    " due to error: " + ex.getMessage());
                            persisted = true; // stop retrying for this row
                        }
                    }
                }

            }
        } catch (Exception ex) {
            LOGGER.warning("Failed to process CSV file: " + ex.getMessage());
            throw new RuntimeException("Failed to process CSV file: " + ex.getMessage(), ex);
        }

        return savedEntities;
    }

    private TestReferenceEntity processRecord(CSVRecord record, User currentUser) {
        // Required fields validation
        String category = getStringOrBlank(record, "Category");
        String testName = getStringOrBlank(record, "Test Name");
        if (category.isEmpty() || testName.isEmpty()) {
            throw new IllegalArgumentException("Record #" + record.getRecordNumber()
                    + ": Category and Test Name are required");
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

        // Handle JSON columns
        // Process ReportJson
        String reportJson = getStringOrBlank(record, "ReportJson");
        if (!reportJson.isEmpty()) {
            // Validate JSON format
            if (isValidJson(reportJson)) {
                entity.setReportJson(reportJson.trim());
            } else {
                LOGGER.warning("Invalid JSON in ReportJson column (record " + record.getRecordNumber() + "): " + reportJson);
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
                LOGGER.warning("Invalid JSON array in ReferenceRanges column (record " + record.getRecordNumber() + "): " + referenceRanges);
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
                LOGGER.warning("Unknown gender '" + genderStr + "' (record "
                        + record.getRecordNumber() + "). Defaulting to MF.");
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
            LOGGER.warning("Invalid number in column '" + column + "' (record "
                    + record.getRecordNumber() + "): " + e.getMessage());
            return null;
        }
    }

    private Integer parseIntWithDefault(CSVRecord record, String column, int defaultValue) {
        String value = getStringOrBlank(record, column);
        if (value.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid integer in column '" + column + "' (record "
                    + record.getRecordNumber() + "). Using default: " + defaultValue);
            return defaultValue;
        }
    }

    private AgeUnit parseAgeUnitWithDefault(CSVRecord record, String column) {
        String value = getStringOrBlank(record, column);
        if (value.isEmpty()) return AgeUnit.YEARS; // Default unit
        try {
            return AgeUnit.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid age unit '" + value + "' (record "
                    + record.getRecordNumber() + "). Defaulting to YEARS.");
            return AgeUnit.YEARS;
        }
    }

    // ------------------ JSON Validation Helper Methods ------------------
    
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
            LOGGER.warning("JSON validation error: " + e.getMessage());
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
            LOGGER.warning("JSON array validation error: " + e.getMessage());
            return false;
        }
    }

    private String generateUniqueReferenceCode(Long labId) {
        int guard = 0;
        while (guard < 1000) {
            String candidate = sequenceGeneratorService.generateCode(labId, EntityType.TEST_REFERENCE);
            if (!testReferenceRepository.existsByTestReferenceCode(candidate)) {
                return candidate;
            }
            guard++;
        }
        throw new IllegalStateException("Unable to generate unique test reference code for lab " + labId);
    }

    private void alignReferenceSequence(Long labId) {
        String prefix = EntityType.TEST_REFERENCE.getPrefix() + labId + "-";
        testReferenceRepository.findTopByTestReferenceCodeStartingWithOrderByTestReferenceCodeDesc(prefix)
                .ifPresent(existing -> {
                    String code = existing.getTestReferenceCode();
                    if (code != null && code.startsWith(prefix)) {
                        String numeric = code.substring(prefix.length());
                        try {
                            long value = Long.parseLong(numeric);
                            sequenceGeneratorService.ensureMinimumSequence(labId, EntityType.TEST_REFERENCE, value);
                        } catch (NumberFormatException ignored) {
                            // legacy format - skip alignment
                        }
                    }
                });
    }

    private void handleReferenceDuplicate(Lab lab, TestReferenceEntity entity, DataIntegrityViolationException dive) {
        if (entity != null && entityManager != null && entityManager.contains(entity)) {
            entityManager.detach(entity);
        }
        LOGGER.warning("Duplicate test_reference_code for lab " + lab.getId() + ". Cause: " + dive.getMostSpecificCause().getMessage());
        alignReferenceSequence(lab.getId());
    }

    private BufferedReader createNormalizedReader(MultipartFile file) throws IOException {
        byte[] fileBytes = file.getBytes();
        String normalizedContent = EncodingUtils.decodeWithUtf8Fallback(fileBytes);
        return new BufferedReader(new StringReader(normalizedContent));
    }
}
