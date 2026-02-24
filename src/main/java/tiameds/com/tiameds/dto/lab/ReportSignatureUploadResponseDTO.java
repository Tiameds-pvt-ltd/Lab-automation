package tiameds.com.tiameds.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportSignatureUploadResponseDTO {
    private String uploadUrl;
    private String fileUrl;
}
