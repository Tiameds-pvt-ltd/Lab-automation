package tiameds.com.tiameds.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportSettingsResponseDTO {
    private Long id;
    private Long labId;
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
    private List<ReportRoleResponseDTO> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
