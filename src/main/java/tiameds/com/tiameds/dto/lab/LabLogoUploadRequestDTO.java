package tiameds.com.tiameds.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabLogoUploadRequestDTO {
    private Long labId;
    private String fileName;
    private String fileType;
}
