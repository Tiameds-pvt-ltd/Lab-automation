package tiameds.com.tiameds.dto.lab;


import lombok.*;
import tiameds.com.tiameds.entity.Gender;

import java.time.LocalDateTime;

@Data
public class TestReferenceDTO {
    private Long id;
    private Long labId;
    private String testReferenceCode;
    private String category;
    private String testName;
    private String testDescription;
    private String units;
    private Gender gender;
    private Double minReferenceRange;
    private Double maxReferenceRange;
    private Integer ageMin;
    private String minAgeUnit;   //maxAgeUnit ,minAgeUnit
    private Integer ageMax;
    private String maxAgeUnit;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // JSON fields
    private String reportJson;
    private String referenceRanges;
    private String dropdown;
    private String impression;
}