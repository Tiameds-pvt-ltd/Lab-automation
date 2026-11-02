package tiameds.com.tiameds.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabRequestDTO {
    private String name;
    private String address;
    private String city;
    private String state;
    private Boolean isActive;
    private String description;

//======================new feilds=========================
private String labLogo;
    private String licenseNumber;
    private String labType;
    private String labZip;
    private String labCountry;
    private String labPhone;
    private String labEmail;
    private String directorName;
    private String directorEmail;
    private String directorPhone;
    private String certificationBody;
    private String labCertificate ;  // Changed to match entity field name
    private String directorGovtId;
    private String labBusinessRegistration;
    private String labLicense;
    private String taxId;
    private String labAccreditation;
    private Boolean dataPrivacyAgreement;

    //======================onboarding fields (all optional, validated in LabCreationService)=========================

    // Organization information (same structure as the onboarding form)
    private OrganizationInfo organization;

    // ---- Additional lab information ----
    private List<String> labServices;
    private Boolean sameAsOrganizationAddress;
    private String labAddressLine2;
    private String labDistrict;

    // ---- Laboratory compliance details ----
    private String establishmentRegistrationNumber;
    private String issuingAuthority;
    private LocalDate issuingDate;
    private LocalDate expiryDate;
    private String labPan;
    private Boolean labPanSameAsOrganization;
    private String labGstin;
    private Boolean labGstinSameAsOrganization;
    private Boolean biomedicalWasteGenerated;
    private String biomedicalWasteAuthorizationNumber;

    // ---- Uploaded documents: URLs returned by /public/onboarding/documents (labLogo is above) ----
    private String gstinCertificate;
    private String establishmentRegistrationCertificate;
    private String biomedicalWasteAuthorizationCertificate;
    private String nablAccreditationCertificate;

    // ---- Review & submit ----
    private Boolean registrationAuthorizationAccepted;
    private Boolean termsAccepted;
    private String termsVersion;

    @Data
    public static class OrganizationInfo {
        private String name;
        // Proprietorship, Partnership, LLP, Private Limited Company, Public Limited Company,
        // Trust, Society, Government Institution, Other
        private String type;
        private String email;
        private String phone;
        private String addressLine1;
        private String addressLine2;
        private String country;
        private String state;
        private String district;
        private String city;
        private String pinCode;
        private String pan;
        private String gstin;
        // URL returned by /public/onboarding/documents (part name: organizationGstinCertificate)
        private String gstinCertificate;
    }
}
