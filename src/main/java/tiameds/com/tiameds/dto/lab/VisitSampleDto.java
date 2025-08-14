package tiameds.com.tiameds.dto.lab;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Data
public class VisitSampleDto {
    private Long visitId;
    private String patientname;
    private String gender;
    private String DateOfBirth;
    private String contactNumber;
    private String email;
    private LocalDate visitDate;
    private String visitStatus;
    private Set<String> sampleNames;
    private List<Long> testIds;
    private List<Long> packageIds;
    private List<VisitTestResultResponseDTO> testResult;


    public VisitSampleDto(Long visitId, String patientname, String gender, String dateOfBirth, String contactNumber, String email, LocalDate visitDate, String visitStatus, String s, Set<String> sampleNames, List<Long> testIds, List<Long> packageIds) {
        this.visitId = visitId;
        this.patientname = patientname;
        this.gender = gender;
        DateOfBirth = dateOfBirth;
        this.contactNumber = contactNumber;
        this.email = email;
        this.visitDate = visitDate;
        this.visitStatus = visitStatus;
        this.sampleNames = sampleNames;
        this.testIds = testIds;
        this.packageIds = packageIds;
        this.testResult = null; // Initialize testResult as null or empty if needed

    }
}







