package tiameds.com.tiameds.services.lab;

import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.entity.EntityType;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.Test;
import tiameds.com.tiameds.repository.TestRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class TestServices {

    private final TestRepository testRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public TestServices(TestRepository testRepository, SequenceGeneratorService sequenceGeneratorService) {
        this.testRepository = testRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    @Transactional
    public List<Test> uploadCSV(MultipartFile file, Lab lab) throws Exception {
        // List to store tests to be saved
        List<Test> tests = new ArrayList<>();

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
                Test test = new Test();
                
                // Generate unique test code using sequence generator
                String testCode = sequenceGeneratorService.generateCode(lab.getId(), EntityType.TEST);
                test.setTestCode(testCode);
                
                test.setCategory(category);
                test.setName(name);
                test.setPrice(price);
                test.getLabs().add(lab);  // Ensure the test is added to the correct lab
                lab.addTest(test);
                
                // Add test to the list for batch saving
                tests.add(test);
            }

            // Save all tests to the database, associating them with the specified lab
            return testRepository.saveAll(tests);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid data in CSV file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error processing CSV file: " + e.getMessage(), e);
        }
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
