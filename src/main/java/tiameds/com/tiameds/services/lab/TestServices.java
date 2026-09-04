package tiameds.com.tiameds.services.lab;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.entity.EntityType;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.Test;
import tiameds.com.tiameds.repository.TestRepository;

import tiameds.com.tiameds.dto.lab.TestDTO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TestServices {

    private final TestRepository testRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @PersistenceContext
    private EntityManager entityManager;

    public TestServices(TestRepository testRepository, SequenceGeneratorService sequenceGeneratorService) {
        this.testRepository = testRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    @Transactional
    public List<Test> uploadCSV(MultipartFile file, Lab lab) throws Exception {
        alignSequenceWithExistingTests(lab.getId());

        List<Test> savedTests = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                Test saved = persistRecord(record, lab);
                if (saved != null) {
                    savedTests.add(saved);
                }
            }

            return savedTests;

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid data in CSV file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error processing CSV file: " + e.getMessage(), e);
        }
    }

    private Test persistRecord(CSVRecord record, Lab lab) {
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

        int attempt = 0;
        while (attempt < 5) {
            attempt++;
            Test test = new Test();
            test.setCategory(category);
            test.setName(name);
            test.setPrice(price);

            String testCode = generateUniqueTestCode(lab.getId());
            test.setTestCode(testCode);
            test.getLabs().add(lab);

            try {
                Test saved = testRepository.save(test);
                lab.addTest(saved);
                return saved;
            } catch (DataIntegrityViolationException dive) {
                handleDuplicateCode(lab, test, dive);
            }
        }

        log.error("Failed to persist test '{}' for lab {} after multiple attempts.", name, lab.getId());
        return null;
    }

    private void handleDuplicateCode(Lab lab, Test test, DataIntegrityViolationException dive) {
        if (entityManager != null && entityManager.contains(test)) {
            entityManager.detach(test);
        }
        log.warn("Duplicate test_code {} detected for lab {}. Attempting to resync sequence. Cause: {}", test.getTestCode(), lab.getId(), dive.getMostSpecificCause().getMessage());
        alignSequenceWithExistingTests(lab.getId());
    }

    private String generateUniqueTestCode(Long labId) {
        int guard = 0;
        while (guard < 1000) {
            String candidate = sequenceGeneratorService.generateCode(labId, EntityType.TEST);
            if (!testRepository.existsByTestCode(candidate)) {
                return candidate;
            }
            guard++;
        }
        throw new IllegalStateException("Unable to generate unique test code for lab " + labId);
    }

    private void alignSequenceWithExistingTests(Long labId) {
        String prefix = EntityType.TEST.getPrefix() + labId + "-";
        testRepository.findTopByTestCodeStartingWithOrderByTestCodeDesc(prefix)
                .ifPresent(existingTest -> {
                    String existingCode = existingTest.getTestCode();
                    if (existingCode != null && existingCode.startsWith(prefix)) {
                        String numericPortion = existingCode.substring(prefix.length());
                        try {
                            long value = Long.parseLong(numericPortion);
                            sequenceGeneratorService.ensureMinimumSequence(labId, EntityType.TEST, value);
                        } catch (NumberFormatException ignored) {
                            // skip sync if legacy code doesn't follow expected format
                        }
                    }
                });
    }





    public List<TestDTO> getAllTests(Lab lab) {
        return lab.getTests().stream()
                .sorted(Comparator.comparingLong(Test::getId))
                .map(test -> new TestDTO(
                        test.getId(),
                        test.getTestCode(),
                        test.getCategory(),
                        test.getName(),
                        test.getPrice(),
                        test.getCreatedAt(),
                        test.getUpdatedAt()
                ))
                .toList();
    }

    public Map<String, Object> getAllTests(Lab lab, int page, int size) {
        return getAllTests(lab, page, size, null, null, null);
    }

    public Map<String, Object> getAllTests(Lab lab, int page, int size, String search, String category, String sortOrder) {
        Sort sort = switch (sortOrder == null ? "" : sortOrder) {
            case "low" -> Sort.by("price").ascending();
            case "high" -> Sort.by("price").descending();
            default -> Sort.by("id").ascending();
        };
        Pageable pageable = PageRequest.of(page, size, sort);

        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim().toLowerCase();
        String normalizedCategory = (category == null || category.isBlank()) ? null : category.trim();

        Specification<Test> spec = (root, query, cb) -> {
            var labJoin = root.join("labs");
            var predicate = cb.equal(labJoin.get("id"), lab.getId());

            if (normalizedSearch != null) {
                String likePattern = "%" + normalizedSearch + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("name")), likePattern),
                        cb.like(cb.lower(root.get("category")), likePattern)
                ));
            }

            if (normalizedCategory != null) {
                predicate = cb.and(predicate, cb.equal(root.get("category"), normalizedCategory));
            }

            return predicate;
        };

        Page<Test> testPage = testRepository.findAll(spec, pageable);

        List<TestDTO> paginatedTests = testPage.getContent().stream()
                .map(test -> new TestDTO(
                        test.getId(),
                        test.getTestCode(),
                        test.getCategory(),
                        test.getName(),
                        test.getPrice(),
                        test.getCreatedAt(),
                        test.getUpdatedAt()
                ))
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", paginatedTests);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", testPage.getTotalElements());
        response.put("totalPages", testPage.getTotalPages());
        response.put("hasNext", testPage.hasNext());
        response.put("hasPrevious", testPage.hasPrevious());
        response.put("categories", testRepository.findDistinctCategoriesByLabId(lab.getId()));

        return response;
    }

    public ResponseEntity<?> downloadCSV(Lab lab) {
        // Fetch all tests associated with the specified lab
        List<Test> tests = testRepository.findByLabs(lab);

        // Generate CSV content
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Category Name,LabTest Name,Price(INR)\n");

        for (Test test : tests) {
            // Ensure proper formatting by escaping commas in values
            csvContent.append("\"").append(test.getCategory().replace("\"", "\"\"")).append("\",");
            csvContent.append("\"").append(test.getName().replace("\"", "\"\"")).append("\",");
            csvContent.append("\"").append(test.getPrice()).append("\"\n");
        }

        // Set the response headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + lab.getName() + "_tests.csv");
        headers.add("Content-Type", "text/csv; charset=UTF-8");

        // Return the CSV content as a ResponseEntity
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent.toString());
    }

}
