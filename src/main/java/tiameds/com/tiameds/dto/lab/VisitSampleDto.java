package tiameds.com.tiameds.dto.lab;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;


@Data
public class VisitSampleDto {
    private Long visitId;
    private String patientname;
    private String gender;
    private String DateOfBirth;
    private String contactNumber;
    private String email;
    private LocalDate visitDate;
    private String visitType;
    private String doctorName;
    private String visitStatus;
    private String visitCode;
    private Set<String> sampleNames;
    private List<TestSummaryDto> tests;
    private List<Long> packageIds;
    private List<VisitTestResultResponseDTO> testResult;


    public VisitSampleDto(Long visitId,
                          String patientname,
                          String gender,
                          String dateOfBirth,
                          String contactNumber,
                          String email,
                          LocalDate visitDate,
                          String visitStatus,
                          String visitType,
                          String doctorName,
                          String visitCode,
                          Set<String> sampleNames, List<TestSummaryDto> tests, List<Long> packageIds,
                          List<VisitTestResultResponseDTO> testResult) {
        this.visitId = visitId;
        this.patientname = patientname;
        this.gender = gender;
        this.DateOfBirth = dateOfBirth;
        this.contactNumber = contactNumber;
        this.email = email;
        this.visitDate = visitDate;
        this.visitStatus = visitStatus;
        this.visitCode = visitCode;
        this.sampleNames = sampleNames;
        this.tests = tests;
        this.packageIds = packageIds;
        this.testResult = testResult;
        this.visitType = visitType;
        this.doctorName = doctorName;
    }



}







