package tiameds.com.tiameds.dto.lab;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RadiologyReportRequest {
    // Optional. When empty the active template for the test (lab override, else global) is used.
    private Long templateId;
    private String clinicalHistory;
    private String technique;
    private String comparison;
    // Structured findings keyed by the template's field keys.
    private JsonNode findingsJson;
    private String impression;
    // Optional on a draft. Required to finalize. null on update = keep the existing signatories.
    private List<Signatory> signatories;
    // Version the client last read; a mismatch means someone else changed the report (409).
    private Long version;
    // Mandatory when changing a FINAL / AMENDED report.
    private String reason;

    @Getter
    @Setter
    public static class Signatory {
        private Long doctorId;
        // REPORTING (default) or VERIFYING
        private String role;
    }
}
