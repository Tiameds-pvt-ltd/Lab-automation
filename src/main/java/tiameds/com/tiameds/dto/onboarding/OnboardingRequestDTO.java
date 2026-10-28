package tiameds.com.tiameds.dto.onboarding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

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

    // Optional: when provided, must match password
    private String confirmPassword;

    // Organization information (optional)
    @Valid
    private OrganizationInfoDTO organization;

    // Lab information
    @Valid
    @NotNull(message = "Lab information is required")
    private LabInfoDTO lab;

    // Review & submit (optional): authorization and Terms & Conditions acceptance
    private Boolean registrationAuthorizationAccepted;
    private Boolean termsAccepted;

    @Size(max = 20, message = "Terms version must be at most 20 characters")
    private String termsVersion;

    @Data
    public static class OrganizationInfoDTO {
        @Size(max = 100, message = "Organization name must be at most 100 characters")
        private String name;

        // Proprietorship, Partnership, LLP, Private Limited Company, Public Limited Company,
        // Trust, Society, Government Institution, Other
        @Size(max = 50, message = "Organization type must be at most 50 characters")
        private String type;

        @Email(message = "Organization email must be valid")
        private String email;

        @Size(max = 20, message = "Organization phone must be at most 20 characters")
        private String phone;

        @Size(max = 100, message = "Organization address line 1 must be at most 100 characters")
        private String addressLine1;

        @Size(max = 100, message = "Organization address line 2 must be at most 100 characters")
        private String addressLine2;

        @Size(max = 100, message = "Organization country must be at most 100 characters")
        private String country;

        @Size(max = 100, message = "Organization state must be at most 100 characters")
        private String state;

        @Size(max = 100, message = "Organization district must be at most 100 characters")
        private String district;

        @Size(max = 50, message = "Organization city must be at most 50 characters")
        private String city;

        @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Organization PIN code must be a valid 6-digit PIN code")
        private String pinCode;

        @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Organization PAN must be a valid PAN (e.g. ABCDE1234F)")
        private String pan;

        @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", message = "Organization GSTIN must be a valid 15-character GSTIN")
        private String gstin;
    }

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

        // ---- Additional lab information (optional) ----
        private List<String> labServices;
        private Boolean sameAsOrganizationAddress;

        @Size(max = 100, message = "Lab address line 2 must be at most 100 characters")
        private String labAddressLine2;

        @Size(max = 100, message = "Lab district must be at most 100 characters")
        private String labDistrict;

        // ---- Laboratory compliance details (optional) ----
        @Size(max = 50, message = "Establishment registration number must be at most 50 characters")
        private String establishmentRegistrationNumber;

        @Size(max = 25, message = "Issuing authority must be at most 25 characters")
        private String issuingAuthority;

        private LocalDate issuingDate;
        private LocalDate expiryDate;
        private String establishmentRegistrationCertificate;

        @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Lab PAN must be a valid PAN (e.g. ABCDE1234F)")
        private String labPan;
        private Boolean labPanSameAsOrganization;

        @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", message = "Lab GSTIN must be a valid 15-character GSTIN")
        private String labGstin;
        private Boolean labGstinSameAsOrganization;
        private String gstinCertificate;

        private Boolean biomedicalWasteGenerated;

        @Size(max = 25, message = "Biomedical waste authorization number must be at most 25 characters")
        private String biomedicalWasteAuthorizationNumber;
        private String biomedicalWasteAuthorizationCertificate;

        // NABL accreditation number is the existing labAccreditation field
        private String nablAccreditationCertificate;
    }
}

