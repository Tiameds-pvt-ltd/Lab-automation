package tiameds.com.tiameds.dto.lab;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Organization and lab details that were entered in the onboarding form.
 * Used both as the response of GET (current values) and as the request body of PUT (edit).
 *
 * Edit semantics (PUT):
 * - A field that is missing / null is left unchanged.
 * - Mandatory fields (name, address, license number, director details, ...) can be changed but not blanked.
 * - Optional fields (organization details, compliance details, PAN / GSTIN, ...) can be cleared by sending "".
 *
 * Not editable here: username / email / password, terms acceptance, approval status and uploaded files
 * (logo and certificates are managed through /lab/admin/{labId}/documents).
 */
@Data
public class LabOnboardingDetailsDTO {

    @Valid
    private OrganizationDetails organization;

    @Valid
    private LabDetails lab;

    @Data
    public static class OrganizationDetails {
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

        // Empty string is allowed so the value can be cleared
        @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "Organization PIN code must be a valid 6-digit PIN code")
        private String pinCode;

        @Pattern(regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Organization PAN must be a valid PAN (e.g. ABCDE1234F)")
        private String pan;

        @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", message = "Organization GSTIN must be a valid 15-character GSTIN")
        private String gstin;
    }

    @Data
    public static class LabDetails {
        // ---- Core lab details (mandatory, cannot be blanked) ----
        private String name;
        private String address;
        private String city;
        private String state;
        private String description;
        private String licenseNumber;
        private String labType;
        private String labZip;
        private String labCountry;
        private String labPhone;

        @Email(message = "Lab email must be valid")
        private String labEmail;

        private String directorName;

        @Email(message = "Director email must be valid")
        private String directorEmail;

        private String directorPhone;

        // NABL accreditation number
        private String labAccreditation;

        // ---- Optional during onboarding (can be cleared with "") ----
        private String certificationBody;
        private String labCertificate;
        private String directorGovtId;
        private String labBusinessRegistration;
        private String labLicense;
        private String taxId;

        // ---- Additional lab information ----
        // Sending a list replaces the stored services; an empty list clears them
        private List<String> labServices;
        private Boolean sameAsOrganizationAddress;

        @Size(max = 100, message = "Lab address line 2 must be at most 100 characters")
        private String labAddressLine2;

        @Size(max = 100, message = "Lab district must be at most 100 characters")
        private String labDistrict;

        // ---- Laboratory compliance details ----
        @Size(max = 50, message = "Establishment registration number must be at most 50 characters")
        private String establishmentRegistrationNumber;

        @Size(max = 25, message = "Issuing authority must be at most 25 characters")
        private String issuingAuthority;

        private LocalDate issuingDate;
        private LocalDate expiryDate;

        @Pattern(regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Lab PAN must be a valid PAN (e.g. ABCDE1234F)")
        private String labPan;
        private Boolean labPanSameAsOrganization;

        @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", message = "Lab GSTIN must be a valid 15-character GSTIN")
        private String labGstin;
        private Boolean labGstinSameAsOrganization;

        private Boolean biomedicalWasteGenerated;

        @Size(max = 25, message = "Biomedical waste authorization number must be at most 25 characters")
        private String biomedicalWasteAuthorizationNumber;
    }
}
