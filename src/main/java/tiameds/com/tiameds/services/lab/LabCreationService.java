package tiameds.com.tiameds.services.lab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.dto.lab.LabCreationResponseDTO;
import tiameds.com.tiameds.dto.lab.LabRequestDTO;
import tiameds.com.tiameds.entity.LabApprovalStatus;
import tiameds.com.tiameds.entity.TempLab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.TempLabRepository;
import tiameds.com.tiameds.services.S3StorageService;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * add-lab for an existing, logged-in user. Same approval as onboarding:
 * the request is stored in temp_lab (PENDING) and the lab is only created in the labs table when a platform admin
 * approves it (see TempLabService.approve). A rejected request stays in temp_lab and no lab is ever created.
 */
@Slf4j
@Service
public class LabCreationService {

    // Allowed values mirror OnboardingService - keep the lists in sync
    private static final Set<String> ORGANIZATION_TYPES = Set.of(
            "Proprietorship", "Partnership", "LLP", "Private Limited Company", "Public Limited Company",
            "Trust", "Society", "Government Institution", "Other");

    private static final Set<String> LAB_SERVICES = Set.of(
            "Clinical Pathology", "Biochemistry", "Haematology", "Microbiology", "Histopathology",
            "Cytopathology", "Serology", "Immunology", "Molecular Diagnostics", "Genetic Testing",
            "Ultrasound", "X-ray", "CT Scan", "MRI", "Blood Banking", "Sample Collection", "All");

    private final LabRepository labRepository;
    private final TempLabRepository tempLabRepository;
    private final S3StorageService s3StorageService;

    public LabCreationService(LabRepository labRepository, TempLabRepository tempLabRepository,
                              S3StorageService s3StorageService) {
        this.labRepository = labRepository;
        this.tempLabRepository = tempLabRepository;
        this.s3StorageService = s3StorageService;
    }

    /**
     * Validates the request and stores it for platform approval. No lab exists until it is approved.
     *
     * @throws IllegalArgumentException if a rule is violated (the controller returns 400)
     */
    @Transactional
    public LabCreationResponseDTO submitLabForApproval(LabRequestDTO dto, User currentUser) {
        requireText("Lab name", dto.getName());
        if (labRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Lab with this name already exists");
        }
        if (tempLabRepository.existsByLabNameIgnoreCaseAndApprovalStatus(dto.getName().trim(), LabApprovalStatus.PENDING)) {
            throw new IllegalArgumentException("A request for a lab with this name is already pending approval");
        }

        validateRequiredFields(dto);
        validateOnboardingFields(dto);

        TempLab temp = new TempLab();
        applyApplicant(temp, currentUser);
        applyLabFields(temp, dto);
        applyOnboardingFields(temp, dto);
        temp.setApprovalStatus(LabApprovalStatus.PENDING);
        temp.setApprovalRequestedAt(LocalDateTime.now());

        TempLab saved = tempLabRepository.save(temp);
        log.info("Lab request {} ({}) submitted for approval by user {}",
                saved.getId(), saved.getLabName(), currentUser.getUsername());

        return LabCreationResponseDTO.builder()
                .message("Lab request submitted successfully. The lab will be created once it is approved.")
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .email(currentUser.getEmail())
                .labName(saved.getLabName())
                .labActive(false)
                .requestId(saved.getId())
                .approvalStatus(saved.getApprovalStatus().name())
                .build();
    }

    /** The requesting user: kept as a reference, the user account itself is not touched. */
    private void applyApplicant(TempLab temp, User user) {
        temp.setExistingUserId(user.getId());
        temp.setEmail(user.getEmail());
        temp.setUsername(user.getUsername());
        temp.setFirstName(user.getFirstName());
        temp.setLastName(user.getLastName());
        temp.setPhone(user.getPhone());
        temp.setAddress(user.getAddress());
        temp.setCity(user.getCity());
        temp.setState(user.getState());
        temp.setZip(user.getZip());
        temp.setCountry(user.getCountry());
        // No password is stored: the account already exists
    }

    /** These are mandatory columns of the stored request (and of the lab it becomes). */
    private void validateRequiredFields(LabRequestDTO dto) {
        requireText("Lab address", dto.getAddress());
        requireText("Lab city", dto.getCity());
        requireText("Lab state", dto.getState());
        requireText("Lab description", dto.getDescription());
        requireText("License number", dto.getLicenseNumber());
        requireText("Lab type", dto.getLabType());
        requireText("Lab zip", dto.getLabZip());
        requireText("Lab country", dto.getLabCountry());
        requireText("Lab phone", dto.getLabPhone());
        requireText("Lab email", dto.getLabEmail());
        requireText("Director name", dto.getDirectorName());
        requireText("Director email", dto.getDirectorEmail());
        requireText("Director phone", dto.getDirectorPhone());
        requireText("Lab accreditation", dto.getLabAccreditation());
    }

    private void applyLabFields(TempLab temp, LabRequestDTO dto) {
        temp.setLabName(dto.getName().trim());
        temp.setLabAddress(dto.getAddress());
        temp.setLabCity(dto.getCity());
        temp.setLabState(dto.getState());
        temp.setLabDescription(dto.getDescription());
        temp.setLabLogo(blankToNull(dto.getLabLogo()));
        temp.setLicenseNumber(dto.getLicenseNumber());
        temp.setLabType(dto.getLabType());
        temp.setLabZip(dto.getLabZip());
        temp.setLabCountry(dto.getLabCountry());
        temp.setLabPhone(dto.getLabPhone());
        temp.setLabEmail(dto.getLabEmail());
        temp.setDirectorName(dto.getDirectorName());
        temp.setDirectorEmail(dto.getDirectorEmail());
        temp.setDirectorPhone(dto.getDirectorPhone());
        temp.setDirectorGovtId(blankToNull(dto.getDirectorGovtId()));
        temp.setCertificationBody(blankToNull(dto.getCertificationBody()));
        temp.setLabCertificate(blankToNull(dto.getLabCertificate()));
        temp.setLabBusinessRegistration(blankToNull(dto.getLabBusinessRegistration()));
        temp.setLabLicense(blankToNull(dto.getLabLicense()));
        temp.setTaxId(blankToNull(dto.getTaxId()));
        temp.setLabAccreditation(dto.getLabAccreditation());
        temp.setDataPrivacyAgreement(Boolean.TRUE.equals(dto.getDataPrivacyAgreement()));
    }

    private static void requireText(String field, String value) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Onboarding fields (same rules as OnboardingService - keep the allowed-value lists in sync)
    // ---------------------------------------------------------------------------------------------

    private void validateOnboardingFields(LabRequestDTO dto) {
        LabRequestFormatValidator.validate(dto);

        LabRequestDTO.OrganizationInfo org = dto.getOrganization();
        if (org != null && !isBlank(org.getType()) && !isAllowed(ORGANIZATION_TYPES, org.getType())) {
            throw new IllegalArgumentException("Invalid organization type: " + org.getType());
        }

        if (dto.getLabServices() != null) {
            for (String service : dto.getLabServices()) {
                if (!isAllowed(LAB_SERVICES, service)) {
                    throw new IllegalArgumentException("Invalid laboratory service: " + service);
                }
            }
        }

        if (dto.getIssuingDate() != null && dto.getExpiryDate() != null
                && dto.getExpiryDate().isBefore(dto.getIssuingDate())) {
            throw new IllegalArgumentException("Expiry date cannot be before issuing date");
        }

        // Documents must be files uploaded through /public/onboarding/documents, not arbitrary URLs
        // (labLogo is left as before, since add-lab already accepted it unchecked)
        if (org != null) {
            requireUploadedDocument("organization.gstinCertificate", org.getGstinCertificate());
        }
        requireUploadedDocument("gstinCertificate", dto.getGstinCertificate());
        requireUploadedDocument("establishmentRegistrationCertificate", dto.getEstablishmentRegistrationCertificate());
        requireUploadedDocument("biomedicalWasteAuthorizationCertificate", dto.getBiomedicalWasteAuthorizationCertificate());
        requireUploadedDocument("nablAccreditationCertificate", dto.getNablAccreditationCertificate());

        // Biomedical waste authorization details are conditionally mandatory
        if (Boolean.TRUE.equals(dto.getBiomedicalWasteGenerated())) {
            if (isBlank(dto.getBiomedicalWasteAuthorizationNumber())) {
                throw new IllegalArgumentException("Biomedical waste authorization number is required when biomedical waste is generated");
            }
            if (isBlank(dto.getBiomedicalWasteAuthorizationCertificate())) {
                throw new IllegalArgumentException("Biomedical waste authorization certificate is required when biomedical waste is generated");
            }
        }
    }

    private void requireUploadedDocument(String field, String url) {
        if (!isBlank(url) && !s3StorageService.isUrlUnderPrefix(url, LabDocumentService.ONBOARDING_S3_PREFIX)) {
            throw new IllegalArgumentException(field + " must be a file uploaded through the onboarding document upload");
        }
    }

    private void applyOnboardingFields(TempLab lab, LabRequestDTO dto) {
        LabRequestDTO.OrganizationInfo org = dto.getOrganization();
        if (org != null) {
            lab.setOrganizationName(blankToNull(org.getName()));
            lab.setOrganizationType(blankToNull(org.getType()));
            lab.setOrganizationEmail(blankToNull(org.getEmail()));
            lab.setOrganizationPhone(blankToNull(org.getPhone()));
            lab.setOrganizationAddressLine1(blankToNull(org.getAddressLine1()));
            lab.setOrganizationAddressLine2(blankToNull(org.getAddressLine2()));
            lab.setOrganizationCountry(blankToNull(org.getCountry()));
            lab.setOrganizationState(blankToNull(org.getState()));
            lab.setOrganizationDistrict(blankToNull(org.getDistrict()));
            lab.setOrganizationCity(blankToNull(org.getCity()));
            lab.setOrganizationPinCode(blankToNull(org.getPinCode()));
            lab.setOrganizationPan(blankToNull(org.getPan()));
            lab.setOrganizationGstin(blankToNull(org.getGstin()));
            lab.setOrganizationGstinCertificate(blankToNull(org.getGstinCertificate()));
        }

        // Lab services (multi-select) stored as comma-separated values
        if (dto.getLabServices() != null && !dto.getLabServices().isEmpty()) {
            lab.setLabServices(dto.getLabServices().stream()
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.joining(",")));
        }

        lab.setSameAsOrganizationAddress(dto.getSameAsOrganizationAddress());
        lab.setLabAddressLine2(blankToNull(dto.getLabAddressLine2()));
        lab.setLabDistrict(blankToNull(dto.getLabDistrict()));
        if (org != null && Boolean.TRUE.equals(dto.getSameAsOrganizationAddress())) {
            if (isBlank(lab.getLabAddressLine2())) {
                lab.setLabAddressLine2(blankToNull(org.getAddressLine2()));
            }
            if (isBlank(lab.getLabDistrict())) {
                lab.setLabDistrict(blankToNull(org.getDistrict()));
            }
        }

        lab.setEstablishmentRegistrationNumber(blankToNull(dto.getEstablishmentRegistrationNumber()));
        lab.setIssuingAuthority(blankToNull(dto.getIssuingAuthority()));
        lab.setIssuingDate(dto.getIssuingDate());
        lab.setExpiryDate(dto.getExpiryDate());

        lab.setLabPanSameAsOrganization(dto.getLabPanSameAsOrganization());
        lab.setLabPan(blankToNull(dto.getLabPan()));
        if (org != null && Boolean.TRUE.equals(dto.getLabPanSameAsOrganization()) && isBlank(lab.getLabPan())) {
            lab.setLabPan(blankToNull(org.getPan()));
        }

        lab.setLabGstinSameAsOrganization(dto.getLabGstinSameAsOrganization());
        lab.setLabGstin(blankToNull(dto.getLabGstin()));
        if (org != null && Boolean.TRUE.equals(dto.getLabGstinSameAsOrganization()) && isBlank(lab.getLabGstin())) {
            lab.setLabGstin(blankToNull(org.getGstin()));
        }

        lab.setBiomedicalWasteGenerated(dto.getBiomedicalWasteGenerated());
        lab.setBiomedicalWasteAuthorizationNumber(blankToNull(dto.getBiomedicalWasteAuthorizationNumber()));

        lab.setGstinCertificate(blankToNull(dto.getGstinCertificate()));
        lab.setEstablishmentRegistrationCertificate(blankToNull(dto.getEstablishmentRegistrationCertificate()));
        lab.setBiomedicalWasteAuthorizationCertificate(blankToNull(dto.getBiomedicalWasteAuthorizationCertificate()));
        lab.setNablAccreditationCertificate(blankToNull(dto.getNablAccreditationCertificate()));

        // Authorization and Terms & Conditions acceptance record (version + timestamp)
        lab.setRegistrationAuthorizationAccepted(dto.getRegistrationAuthorizationAccepted());
        lab.setTermsAccepted(dto.getTermsAccepted());
        if (Boolean.TRUE.equals(dto.getTermsAccepted())) {
            lab.setTermsVersion(blankToNull(dto.getTermsVersion()));
            lab.setTermsAcceptedAt(LocalDateTime.now());
        }
    }

    private static boolean isAllowed(Set<String> allowedValues, String value) {
        return value != null && allowedValues.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(value.trim()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}


