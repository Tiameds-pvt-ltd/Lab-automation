package tiameds.com.tiameds.dto.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OnboardingRejectRequestDTO {

    @NotBlank(message = "Rejection reason is required")
    @Size(max = 1000, message = "Rejection reason must be at most 1000 characters")
    private String reason;
}
