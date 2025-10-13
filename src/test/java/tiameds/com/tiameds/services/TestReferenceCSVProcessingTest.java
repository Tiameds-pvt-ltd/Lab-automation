package tiameds.com.tiameds.services;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TestReferenceCSVProcessingTest {

    @Test
    public void testCSVProcessingWithJSONColumns() throws Exception {
        // Read the sample CSV file
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        getClass().getClassLoader().getResourceAsStream("sample_test_references_with_reference_ranges.csv"),
                        StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .withFirstRecordAsHeader()
                     .withIgnoreHeaderCase()
                     .withTrim())) {

            int recordCount = 0;
            for (CSVRecord record : csvParser) {
                recordCount++;
                
                // Test that we can read the JSON columns
                String reportJson = getStringOrBlank(record, "ReportJson");
                String referenceRanges = getStringOrBlank(record, "ReferenceRanges");
                
                System.out.println("Record " + recordCount + ":");
                System.out.println("  Test Name: " + getStringOrBlank(record, "Test Name"));
                System.out.println("  ReportJson: " + (reportJson.isEmpty() ? "Empty" : "Present (" + reportJson.length() + " chars)"));
                System.out.println("  ReferenceRanges: " + (referenceRanges.isEmpty() ? "Empty" : "Present (" + referenceRanges.length() + " chars)"));
                
                // Validate JSON structure if present
                if (!reportJson.isEmpty()) {
                    assertTrue(isValidJson(reportJson), "ReportJson should be valid JSON for record " + recordCount);
                }
                
                if (!referenceRanges.isEmpty()) {
                    assertTrue(isValidJsonArray(referenceRanges), "ReferenceRanges should be valid JSON array for record " + recordCount);
                }
            }
            
            // Should have processed multiple records
            assertTrue(recordCount > 0, "Should have processed at least one record");
            System.out.println("Successfully processed " + recordCount + " records from CSV");
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
    
    private boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        try {
            String trimmed = jsonString.trim();
            return trimmed.startsWith("{") && trimmed.endsWith("}");
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isValidJsonArray(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        try {
            String trimmed = jsonString.trim();
            return trimmed.startsWith("[") && trimmed.endsWith("]");
        } catch (Exception e) {
            return false;
        }
    }
}
