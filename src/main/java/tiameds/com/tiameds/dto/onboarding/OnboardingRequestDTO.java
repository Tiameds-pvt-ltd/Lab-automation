package tiameds.com.tiameds.dto.onboarding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for onboarding form submission
 * Includes user details and lab creation information
 */
@Data
public class OnboardingRequestDTO {

    // Verification token (required to ensure user came from verified email)
    @NotBlank(message = "Verification token is required")
    private String token;

    // User information
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Zip is required")
    private String zip;

    @NotBlank(message = "Country is required")
    private String country;

    // Lab information
    @Valid
    @NotNull(message = "Lab information is required")
    private LabInfoDTO lab;

    @Data
    public static class LabInfoDTO {
        @NotBlank(message = "Lab name is required")
        private String name;

        @NotBlank(message = "Lab address is required")
        private String address;

        @NotBlank(message = "Lab city is required")
        private String city;

        @NotBlank(message = "Lab state is required")
        private String state;

        @NotBlank(message = "Lab description is required")
        private String description;

        private String labLogo;
        
        @NotBlank(message = "License number is required")
        private String licenseNumber;

        @NotBlank(message = "Lab type is required")
        private String labType;

        @NotBlank(message = "Lab zip is required")
        private String labZip;

        @NotBlank(message = "Lab country is required")
        private String labCountry;

        @NotBlank(message = "Lab phone is required")
        private String labPhone;

        @NotBlank(message = "Lab email is required")
        @Email(message = "Lab email must be valid")
        private String labEmail;

        @NotBlank(message = "Director name is required")
        private String directorName;

        @NotBlank(message = "Director email is required")
        @Email(message = "Director email must be valid")
        private String directorEmail;

        @NotBlank(message = "Director phone is required")
        private String directorPhone;

        private String certificationBody;
        private String labCertificate;
        private String directorGovtId;
        private String labBusinessRegistration;
        private String labLicense;
        private String taxId;
        private String labAccreditation;
        
        @NotNull(message = "Data privacy agreement is required")
        private Boolean dataPrivacyAgreement;
    }
}

