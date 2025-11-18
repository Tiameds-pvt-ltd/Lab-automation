package tiameds.com.tiameds.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for verification token validation response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponseDTO {
    private boolean valid;
    private String email;
    private String message;
    private String redirectUrl; // Frontend onboarding form URL
}

