package tiameds.com.tiameds.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for lab creation response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabCreationResponseDTO {
    private String message;
    private Long userId;
    private String username;
    private String email;
    private Long labId;
    private String labName;
    private boolean labActive;
}

