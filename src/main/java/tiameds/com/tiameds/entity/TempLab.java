package tiameds.com.tiameds.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * An onboarding submission waiting for platform review.
 * The user and lab are created from it only on approval; a rejected request stays here and nowhere else.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "temp_lab")
public class TempLab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //    ======================applicant=========================

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    // BCrypt hash, never the plain password
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "zip")
    private String zip;

    @Column(name = "country")
    private String country;

    //    ======================organization=========================

    @Column(name = "organization_name", length = 100)
    private String organizationName;

    @Column(name = "organization_type", length = 50)
    private String organizationType;

    @Column(name = "organization_email")
    private String organizationEmail;

    @Column(name = "organization_phone", length = 20)
    private String organizationPhone;

    @Column(name = "organization_address_line1", length = 100)
    private String organizationAddressLine1;

    @Column(name = "organization_address_line2", length = 100)
    private String organizationAddressLine2;

    @Column(name = "organization_country", length = 100)
    private String organizationCountry;

    @Column(name = "organization_state", length = 100)
    private String organizationState;

    @Column(name = "organization_district", length = 100)
    private String organizationDistrict;

    @Column(name = "organization_city", length = 50)
    private String organizationCity;

    @Column(name = "organization_pin_code", length = 10)
    private String organizationPinCode;

    @Column(name = "organization_pan", length = 10)
    private String organizationPan;

    @Column(name = "organization_gstin", length = 15)
    private String organizationGstin;

    @Column(name = "organization_gstin_certificate")
    private String organizationGstinCertificate;

    //    ======================lab=========================

    @Column(name = "lab_name", nullable = false)
    private String labName;

    @Column(name = "lab_address", nullable = false)
    private String labAddress;

    @Column(name = "lab_city", nullable = false)
    private String labCity;

    @Column(name = "lab_state", nullable = false)
    private String labState;

    @Column(name = "lab_description", nullable = false)
    private String labDescription;

    @Column(name = "lab_logo")
    private String labLogo;

    @Column(name = "license_number", nullable = false)
    private String licenseNumber;

    @Column(name = "lab_type", nullable = false)
    private String labType;

    @Column(name = "lab_zip", nullable = false)
    private String labZip;

    @Column(name = "lab_country", nullable = false)
    private String labCountry;

    @Column(name = "lab_phone", nullable = false)
    private String labPhone;

    @Column(name = "lab_email", nullable = false)
    private String labEmail;

    @Column(name = "director_name", nullable = false)
    private String directorName;

    @Column(name = "director_email", nullable = false)
    private String directorEmail;

    @Column(name = "director_phone", nullable = false)
    private String directorPhone;

    @Column(name = "certification_body")
    private String certificationBody;

    @Column(name = "lab_certificate")
    private String labCertificate;

    @Column(name = "director_govt_id")
    private String directorGovtId;

    @Column(name = "lab_business_registration")
    private String labBusinessRegistration;

    @Column(name = "lab_license")
    private String labLicense;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "lab_accreditation", nullable = false)
    private String labAccreditation;

    @Column(name = "data_privacy_agreement", nullable = false)
    private Boolean dataPrivacyAgreement;

    // Comma-separated list of selected laboratory services
    @Column(name = "lab_services", columnDefinition = "TEXT")
    private String labServices;

    @Column(name = "same_as_organization_address")
    private Boolean sameAsOrganizationAddress;

    @Column(name = "lab_address_line2", length = 100)
    private String labAddressLine2;

    @Column(name = "lab_district", length = 100)
    private String labDistrict;

    //    ======================compliance=========================

    @Column(name = "establishment_registration_number", length = 50)
    private String establishmentRegistrationNumber;

    @Column(name = "issuing_authority", length = 25)
    private String issuingAuthority;

    @Column(name = "issuing_date")
    private LocalDate issuingDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "establishment_registration_certificate")
    private String establishmentRegistrationCertificate;

    @Column(name = "lab_pan", length = 10)
    private String labPan;

    @Column(name = "lab_pan_same_as_organization")
    private Boolean labPanSameAsOrganization;

    @Column(name = "lab_gstin", length = 15)
    private String labGstin;

    @Column(name = "lab_gstin_same_as_organization")
    private Boolean labGstinSameAsOrganization;

    @Column(name = "gstin_certificate")
    private String gstinCertificate;

    @Column(name = "biomedical_waste_generated")
    private Boolean biomedicalWasteGenerated;

    @Column(name = "biomedical_waste_authorization_number", length = 25)
    private String biomedicalWasteAuthorizationNumber;

    @Column(name = "biomedical_waste_authorization_certificate")
    private String biomedicalWasteAuthorizationCertificate;

    @Column(name = "nabl_accreditation_certificate")
    private String nablAccreditationCertificate;

    //    ======================authorization and terms=========================

    @Column(name = "registration_authorization_accepted")
    private Boolean registrationAuthorizationAccepted;

    @Column(name = "terms_accepted")
    private Boolean termsAccepted;

    @Column(name = "terms_version", length = 20)
    private String termsVersion;

    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    //    ======================review=========================

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private LabApprovalStatus approvalStatus = LabApprovalStatus.PENDING;

    @Column(name = "approval_requested_at", nullable = false)
    private LocalDateTime approvalRequestedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Set on approval: the user / lab that were created from this request
    // Set for add-lab requests (an existing user asking for another lab); null for onboarding requests
    @Column(name = "existing_user_id")
    private Long existingUserId;

    @Column(name = "approved_user_id")
    private Long approvedUserId;

    @Column(name = "approved_lab_id")
    private Long approvedLabId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
