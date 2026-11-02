package tiameds.com.tiameds.services.lab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.dto.lab.LabCreationResponseDTO;
import tiameds.com.tiameds.dto.lab.LabRequestDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.services.S3StorageService;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for handling lab creation with user association.
 * Similar to onboarding flow but for existing authenticated users.
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
    private final UserLabService userLabService;
    private final S3StorageService s3StorageService;

    public LabCreationService(LabRepository labRepository, UserLabService userLabService,
                              S3StorageService s3StorageService) {
        this.s3StorageService = s3StorageService;
        this.labRepository = labRepository;
        this.userLabService = userLabService;
    }

    /**
     * Creates a lab and associates the user with it.
     * 
     * Flow:
     * 1. Validates lab name doesn't already exist
     * 2. Creates Lab entity with all provided information
     * 3. Associates user with the lab
     * 4. Saves lab to database
     * 
     * This is all done in a single transaction to ensure atomicity.
     *
     * @param labRequestDTO The lab request containing lab information
     * @param currentUser The authenticated user creating the lab
     * @return LabCreationResponseDTO with created lab and user details
     */
    @Transactional
    public LabCreationResponseDTO createLabWithUserAssociation(LabRequestDTO labRequestDTO, User currentUser) {
        // Step 1: Check if the lab already exists
        if (userLabService.existsLabByName(labRequestDTO.getName())) {
            throw new IllegalArgumentException("Lab with this name already exists");
        }

        // Step 2: Create Lab entity
        Lab lab = new Lab();
        
        // Basic fields
        lab.setName(labRequestDTO.getName());
        lab.setAddress(labRequestDTO.getAddress());
        lab.setCity(labRequestDTO.getCity());
        lab.setState(labRequestDTO.getState());
        lab.setDescription(labRequestDTO.getDescription());
        lab.setIsActive(true);
        lab.setCreatedBy(currentUser);

        // Business fields
        lab.setLabLogo(labRequestDTO.getLabLogo());
        lab.setLicenseNumber(labRequestDTO.getLicenseNumber());
        lab.setLabType(labRequestDTO.getLabType());
        lab.setLabZip(labRequestDTO.getLabZip());
        lab.setLabCountry(labRequestDTO.getLabCountry());
        lab.setLabPhone(labRequestDTO.getLabPhone());
        lab.setLabEmail(labRequestDTO.getLabEmail());

        // Director fields
        lab.setDirectorName(labRequestDTO.getDirectorName());
        lab.setDirectorEmail(labRequestDTO.getDirectorEmail());
        lab.setDirectorPhone(labRequestDTO.getDirectorPhone());
        lab.setDirectorGovtId(labRequestDTO.getDirectorGovtId());

        // Compliance fields
        lab.setCertificationBody(labRequestDTO.getCertificationBody());
        lab.setLabCertificate(labRequestDTO.getLabCertificate());
        lab.setLabBusinessRegistration(labRequestDTO.getLabBusinessRegistration());
        lab.setLabLicense(labRequestDTO.getLabLicense());
        lab.setTaxId(labRequestDTO.getTaxId());
        lab.setLabAccreditation(labRequestDTO.getLabAccreditation());
        lab.setDataPrivacyAgreement(labRequestDTO.getDataPrivacyAgreement() != null && labRequestDTO.getDataPrivacyAgreement());

        // Onboarding fields (organization, additional lab info, compliance, terms)
        validateOnboardingFields(labRequestDTO);
        applyOnboardingFields(lab, labRequestDTO);

        // Step 3: Save lab to database
        Lab savedLab = labRepository.save(lab);
        log.info("Created lab: {} (ID: {})", savedLab.getName(), savedLab.getId());

        // Step 4: Add user as member of the lab
        savedLab.getMembers().add(currentUser);
        labRepository.save(savedLab);
        log.info("Added user {} as member of lab {}", currentUser.getUsername(), savedLab.getName());

        // Step 5: Build response
        return LabCreationResponseDTO.builder()
                .message("Lab created successfully and user added as a member")
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .email(currentUser.getEmail())
                .labId(savedLab.getId())
                .labName(savedLab.getName())
                .labActive(savedLab.getIsActive())
                .build();
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

    private void applyOnboardingFields(Lab lab, LabRequestDTO dto) {
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


