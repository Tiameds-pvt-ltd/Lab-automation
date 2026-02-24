package tiameds.com.tiameds.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportRoleResponseDTO {
    private Long id;
    private String role;
    private String displayName;
    private String designation;
    private String signatureUrl;
    private Boolean enabled;
    private Integer sortOrder;
}
