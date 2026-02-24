package tiameds.com.tiameds.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportSettingsPayloadDTO {
    private String templateId;
    private Boolean headerEnabled;
    private Boolean headerRequired;
    private Integer fontSize;
    private String textSize;
    private String textColor;
    private String signaturePlacement;
    private Integer signatureColumns;
    private Boolean disclaimerEnabled;
    private String disclaimerText;
    private List<ReportRolePayloadDTO> roles;
}
