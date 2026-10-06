package tiameds.com.tiameds.services.lab;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.dto.lab.TestsMasterDTO;
import tiameds.com.tiameds.entity.TestsMaster;
import tiameds.com.tiameds.repository.TestsMasterRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TestsMasterService {

    private final TestsMasterRepository testsMasterRepository;

    public TestsMasterService(TestsMasterRepository testsMasterRepository) {
        this.testsMasterRepository = testsMasterRepository;
    }

    public List<TestsMasterDTO> getAllTests() {
        return testsMasterRepository.findAllByIsActive(1)
                .stream()
                .sorted((a, b) -> Integer.compare(a.getTestId(), b.getTestId()))
                .map(this::toDTO)
                .toList();
    }

    public Map<String, Object> getAllTests(int page, int size, String search, String category, String sortOrder) {
        Sort sort = switch (sortOrder == null ? "" : sortOrder) {
            case "low" -> Sort.by("price").ascending();
            case "high" -> Sort.by("price").descending();
            default -> Sort.by("testId").ascending();
        };
        Pageable pageable = PageRequest.of(page, size, sort);

        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim().toLowerCase();
        String normalizedCategory = (category == null || category.isBlank()) ? null : category.trim();

        Specification<TestsMaster> spec = (root, query, cb) -> {
            var predicate = cb.equal(root.get("isActive"), 1);

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

        Page<TestsMaster> testPage = testsMasterRepository.findAll(spec, pageable);

        List<TestsMasterDTO> content = testPage.getContent().stream()
                .map(this::toDTO)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", testPage.getTotalElements());
        response.put("totalPages", testPage.getTotalPages());
        response.put("hasNext", testPage.hasNext());
        response.put("hasPrevious", testPage.hasPrevious());
        response.put("categories", testsMasterRepository.findDistinctActiveCategories());

        return response;
    }

    @Transactional
    public List<TestsMaster> uploadCSV(MultipartFile file) throws Exception {
        List<TestsMaster> saved = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                String category = record.get("Category Name");
                String name = record.get("LabTest Name");
                String priceString = record.get("Price(INR)");

                if (category == null || name == null || priceString == null
                        || category.isBlank() || name.isBlank() || priceString.isBlank()) {
                    log.warn("Skipping CSV row with missing fields: {}", record);
                    continue;
                }

                if (testsMasterRepository.existsByName(name)) {
                    log.warn("Skipping duplicate test name in master: {}", name);
                    continue;
                }

                BigDecimal price;
                try {
                    price = new BigDecimal(priceString.trim());
                } catch (NumberFormatException e) {
                    log.warn("Skipping row with invalid price '{}': {}", priceString, record);
                    continue;
                }

                TestsMaster test = new TestsMaster();
                test.setName(name.trim());
                test.setCategory(category.trim());
                test.setPrice(price);
                test.setIsActive(1);
                // Save to get auto-generated ID, then set test_code based on it
                test.setTestCode("TMP-" + System.nanoTime());
                TestsMaster persisted = testsMasterRepository.save(test);
                persisted.setTestCode(String.format("MSTR-%05d", persisted.getTestId()));
                testsMasterRepository.save(persisted);
                saved.add(persisted);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error processing CSV file: " + e.getMessage(), e);
        }

        return saved;
    }

    public ResponseEntity<?> downloadCSV() {
        List<TestsMaster> tests = testsMasterRepository.findAllByIsActive(1);

        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Category Name,LabTest Name,Price(INR)\n");

        for (TestsMaster test : tests) {
            csvContent.append("\"").append(test.getCategory().replace("\"", "\"\"")).append("\",");
            csvContent.append("\"").append(test.getName().replace("\"", "\"\"")).append("\",");
            csvContent.append("\"").append(test.getPrice()).append("\"\n");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=tests_master.csv");
        headers.add("Content-Type", "text/csv; charset=UTF-8");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent.toString());
    }

    public TestsMasterDTO toDTO(TestsMaster test) {
        return new TestsMasterDTO(
                test.getTestId(),
                test.getTestCode(),
                test.getName(),
                test.getCategory(),
                test.getPrice(),
                test.getCreatedAt(),
                test.getUpdatedAt(),
                test.getIsActive()
        );
    }
}
