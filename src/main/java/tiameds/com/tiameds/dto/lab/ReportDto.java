package tiameds.com.tiameds.dto.lab;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReportDto {

    @JsonProperty("report_id")
    private Long reportId;

    @JsonProperty("visit_id")
    private Long visitId;

    @JsonProperty("testName")
    private String testName;

    @JsonProperty("testCategory")
    private String testCategory;

    @JsonProperty("patientName")
    private String patientName; // Added to match Postman request

    @JsonProperty("lab_id")
    private Long labId;

    @JsonProperty("referenceDescription")
    private String referenceDescription;

    @JsonProperty("referenceRange")
    private String referenceRange;

    @JsonProperty("referenceAgeRange") // Fixed typo from `referenceAgerange`
    private String referenceAgeRange;

    @JsonProperty("enteredValue")
    private String enteredValue;

    @JsonProperty("unit")
    private String unit;

    @JsonProperty("created_by")
    private Long createdBy;

    //-------
    @JsonProperty("description")
    private String description;

    @JsonProperty("remarks")
    private String remarks;

    @JsonProperty("comments")
    private String comments;

    // JSON fields
    @JsonProperty("reportJson")
    private String reportJson;

    @JsonProperty("referenceRanges")
    private String referenceRanges;
}
