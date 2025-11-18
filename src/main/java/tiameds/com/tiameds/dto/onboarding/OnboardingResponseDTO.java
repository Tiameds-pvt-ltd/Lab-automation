package tiameds.com.tiameds.dto.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for onboarding completion response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingResponseDTO {
    private String message;
    private Long userId;
    private String username;
    private String email;
    private Long labId;
    private String labName;
    private boolean accountActive;
}

