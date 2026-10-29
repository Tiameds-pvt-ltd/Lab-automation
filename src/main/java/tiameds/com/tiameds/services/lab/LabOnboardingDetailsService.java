package tiameds.com.tiameds.services.lab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.dto.lab.LabOnboardingDetailsDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.UserRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

/**
 * View and edit of the organization and lab details that were submitted during onboarding.
 *
 * - Only a member of the lab can view / edit it (same rule as the lab documents).
 * - A field that is not sent (null) is never touched, so a partial edit cannot wipe other data.
 * - Validation runs on the merged result and the whole edit is one transaction: if any rule fails,
 *   nothing is saved.
 * - Uploaded files (logo, certificates) are not handled here, see {@link LabDocumentService}.
 */
@Slf4j
@Service
public class LabOnboardingDetailsService {

    // Allowed values mirror OnboardingService - keep both lists in sync
    private static final Set<String> ORGANIZATION_TYPES = Set.of(
            "Proprietorship", "Partnership", "LLP", "Private Limited Company", "Public Limited Company",
            "Trust", "Society", "Government Institution", "Other");

    private static final Set<String> LAB_SERVICES = Set.of(
            "Clinical Pathology", "Biochemistry", "Haematology", "Microbiology", "Histopathology",
            "Cytopathology", "Serology", "Immunology", "Molecular Diagnostics", "Genetic Testing",
            "Ultrasound", "X-ray", "CT Scan", "MRI", "Blood Banking", "Sample Collection", "All");

    private final LabRepository labRepository;
    private final UserRepository userRepository;

    public LabOnboardingDetailsService(LabRepository labRepository, UserRepository userRepository) {
        this.labRepository = labRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns the current organization and lab details (used to pre-fill the edit form).
     */
    @Transactional(readOnly = true)
    public LabOnboardingDetailsDTO getDetails(User currentUser, Long labId) {
        return toDto(getAccessibleLab(currentUser, labId));
    }

    /**
     * Applies the edit and returns the saved details.
     *
     * @throws IllegalArgumentException if nothing was sent or a business rule is violated (maps to 400)
     * @throws NoSuchElementException   if the lab does not exist (maps to 404)
     * @throws IllegalStateException    if the user is not a member of the lab (maps to 401)
     */
    @Transactional
    public LabOnboardingDetailsDTO updateDetails(User currentUser, Long labId, LabOnboardingDetailsDTO request) {
        Lab lab = getAccessibleLab(currentUser, labId);
        if (request == null || (request.getOrganization() == null && request.getLab() == null)) {
            throw new IllegalArgumentException("Organization or lab details are required");
        }

        applyOrganization(lab, request.getOrganization());
        applyLab(lab, request.getLab());
        applySameAsOrganization(lab);
        validateMergedState(lab);

        Lab saved = labRepository.save(lab);
        log.info("Onboarding details updated for lab {} by user {}", saved.getId(), currentUser.getUsername());
        return toDto(saved);
    }

    // ---------------------------------------------------------------------------------------------
    // Apply
    // ---------------------------------------------------------------------------------------------

    private void applyOrganization(Lab lab, LabOnboardingDetailsDTO.OrganizationDetails org) {
        if (org == null) {
            return;
        }
        if (!isBlank(org.getType()) && !isAllowed(ORGANIZATION_TYPES, org.getType())) {
            throw new IllegalArgumentException("Invalid organization type: " + org.getType());
        }

        setOptional(org.getName(), lab::setOrganizationName);
        setOptional(org.getType(), lab::setOrganizationType);
        setOptional(org.getEmail(), lab::setOrganizationEmail);
        setOptional(org.getPhone(), lab::setOrganizationPhone);
        setOptional(org.getAddressLine1(), lab::setOrganizationAddressLine1);
        setOptional(org.getAddressLine2(), lab::setOrganizationAddressLine2);
        setOptional(org.getCountry(), lab::setOrganizationCountry);
        setOptional(org.getState(), lab::setOrganizationState);
        setOptional(org.getDistrict(), lab::setOrganizationDistrict);
        setOptional(org.getCity(), lab::setOrganizationCity);
        setOptional(org.getPinCode(), lab::setOrganizationPinCode);
        setOptional(org.getPan(), lab::setOrganizationPan);
        setOptional(org.getGstin(), lab::setOrganizationGstin);
    }

    private void applyLab(Lab lab, LabOnboardingDetailsDTO.LabDetails details) {
        if (details == null) {
            return;
        }

        // Core details: columns are NOT NULL, so they can be changed but never blanked
        setRequired("Lab name", details.getName(), lab::setName);
        setRequired("Lab address", details.getAddress(), lab::setAddress);
        setRequired("Lab city", details.getCity(), lab::setCity);
        setRequired("Lab state", details.getState(), lab::setState);
        setRequired("Lab description", details.getDescription(), lab::setDescription);
        setRequired("License number", details.getLicenseNumber(), lab::setLicenseNumber);
        setRequired("Lab type", details.getLabType(), lab::setLabType);
        setRequired("Lab zip", details.getLabZip(), lab::setLabZip);
        setRequired("Lab country", details.getLabCountry(), lab::setLabCountry);
        setRequired("Lab phone", details.getLabPhone(), lab::setLabPhone);
        setRequired("Lab email", details.getLabEmail(), lab::setLabEmail);
        setRequired("Director name", details.getDirectorName(), lab::setDirectorName);
        setRequired("Director email", details.getDirectorEmail(), lab::setDirectorEmail);
        setRequired("Director phone", details.getDirectorPhone(), lab::setDirectorPhone);
        setRequired("Lab accreditation", details.getLabAccreditation(), lab::setLabAccreditation);

        // Optional during onboarding, so they can also be cleared
        setOptional(details.getCertificationBody(), lab::setCertificationBody);
        setOptional(details.getLabCertificate(), lab::setLabCertificate);
        setOptional(details.getDirectorGovtId(), lab::setDirectorGovtId);
        setOptional(details.getLabBusinessRegistration(), lab::setLabBusinessRegistration);
        setOptional(details.getLabLicense(), lab::setLabLicense);
        setOptional(details.getTaxId(), lab::setTaxId);

        // Lab services (multi-select) are stored as comma-separated values, same as onboarding
        if (details.getLabServices() != null) {
            Set<String> services = new LinkedHashSet<>();
            for (String service : details.getLabServices()) {
                if (!isAllowed(LAB_SERVICES, service)) {
                    throw new IllegalArgumentException("Invalid laboratory service: " + service);
                }
                services.add(service.trim());
            }
            lab.setLabServices(services.isEmpty() ? null : String.join(",", services));
        }

        if (details.getSameAsOrganizationAddress() != null) {
            lab.setSameAsOrganizationAddress(details.getSameAsOrganizationAddress());
        }
        setOptional(details.getLabAddressLine2(), lab::setLabAddressLine2);
        setOptional(details.getLabDistrict(), lab::setLabDistrict);

        // Compliance details
        setOptional(details.getEstablishmentRegistrationNumber(), lab::setEstablishmentRegistrationNumber);
        setOptional(details.getIssuingAuthority(), lab::setIssuingAuthority);
        if (details.getIssuingDate() != null) {
            lab.setIssuingDate(details.getIssuingDate());
        }
        if (details.getExpiryDate() != null) {
            lab.setExpiryDate(details.getExpiryDate());
        }

        if (details.getLabPanSameAsOrganization() != null) {
            lab.setLabPanSameAsOrganization(details.getLabPanSameAsOrganization());
        }
        setOptional(details.getLabPan(), lab::setLabPan);

        if (details.getLabGstinSameAsOrganization() != null) {
            lab.setLabGstinSameAsOrganization(details.getLabGstinSameAsOrganization());
        }
        setOptional(details.getLabGstin(), lab::setLabGstin);

        if (details.getBiomedicalWasteGenerated() != null) {
            lab.setBiomedicalWasteGenerated(details.getBiomedicalWasteGenerated());
        }
        setOptional(details.getBiomedicalWasteAuthorizationNumber(), lab::setBiomedicalWasteAuthorizationNumber);
    }

    /**
     * Same "same as organization" behaviour as onboarding: the lab value is copied from the organization
     * only when the lab value is blank, so an explicitly entered lab value is never overwritten.
     */
    private void applySameAsOrganization(Lab lab) {
        if (Boolean.TRUE.equals(lab.getSameAsOrganizationAddress())) {
            if (isBlank(lab.getLabAddressLine2())) {
                lab.setLabAddressLine2(lab.getOrganizationAddressLine2());
            }
            if (isBlank(lab.getLabDistrict())) {
                lab.setLabDistrict(lab.getOrganizationDistrict());
            }
        }
        if (Boolean.TRUE.equals(lab.getLabPanSameAsOrganization()) && isBlank(lab.getLabPan())) {
            lab.setLabPan(lab.getOrganizationPan());
        }
        if (Boolean.TRUE.equals(lab.getLabGstinSameAsOrganization()) && isBlank(lab.getLabGstin())) {
            lab.setLabGstin(lab.getOrganizationGstin());
        }
    }

    /**
     * Cross-field rules, checked against the merged state so that a partial edit
     * (e.g. only the expiry date) is validated against the values already stored.
     */
    private void validateMergedState(Lab lab) {
        if (lab.getIssuingDate() != null && lab.getExpiryDate() != null
                && lab.getExpiryDate().isBefore(lab.getIssuingDate())) {
            throw new IllegalArgumentException("Expiry date cannot be before issuing date");
        }

        // Biomedical waste authorization details are conditionally mandatory
        if (Boolean.TRUE.equals(lab.getBiomedicalWasteGenerated())) {
            if (isBlank(lab.getBiomedicalWasteAuthorizationNumber())) {
                throw new IllegalArgumentException("Biomedical waste authorization number is required when biomedical waste is generated");
            }
            if (isBlank(lab.getBiomedicalWasteAuthorizationCertificate())) {
                throw new IllegalArgumentException("Biomedical waste authorization certificate is required when biomedical waste is generated. "
                        + "Upload it first through the lab documents endpoint");
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------------------------------------

    private LabOnboardingDetailsDTO toDto(Lab lab) {
        LabOnboardingDetailsDTO.OrganizationDetails org = new LabOnboardingDetailsDTO.OrganizationDetails();
        org.setName(lab.getOrganizationName());
        org.setType(lab.getOrganizationType());
        org.setEmail(lab.getOrganizationEmail());
        org.setPhone(lab.getOrganizationPhone());
        org.setAddressLine1(lab.getOrganizationAddressLine1());
        org.setAddressLine2(lab.getOrganizationAddressLine2());
        org.setCountry(lab.getOrganizationCountry());
        org.setState(lab.getOrganizationState());
        org.setDistrict(lab.getOrganizationDistrict());
        org.setCity(lab.getOrganizationCity());
        org.setPinCode(lab.getOrganizationPinCode());
        org.setPan(lab.getOrganizationPan());
        org.setGstin(lab.getOrganizationGstin());

        LabOnboardingDetailsDTO.LabDetails details = new LabOnboardingDetailsDTO.LabDetails();
        details.setName(lab.getName());
        details.setAddress(lab.getAddress());
        details.setCity(lab.getCity());
        details.setState(lab.getState());
        details.setDescription(lab.getDescription());
        details.setLicenseNumber(lab.getLicenseNumber());
        details.setLabType(lab.getLabType());
        details.setLabZip(lab.getLabZip());
        details.setLabCountry(lab.getLabCountry());
        details.setLabPhone(lab.getLabPhone());
        details.setLabEmail(lab.getLabEmail());
        details.setDirectorName(lab.getDirectorName());
        details.setDirectorEmail(lab.getDirectorEmail());
        details.setDirectorPhone(lab.getDirectorPhone());
        details.setCertificationBody(lab.getCertificationBody());
        details.setLabCertificate(lab.getLabCertificate());
        details.setDirectorGovtId(lab.getDirectorGovtId());
        details.setLabBusinessRegistration(lab.getLabBusinessRegistration());
        details.setLabLicense(lab.getLabLicense());
        details.setTaxId(lab.getTaxId());
        details.setLabAccreditation(lab.getLabAccreditation());
        details.setLabServices(splitServices(lab.getLabServices()));
        details.setSameAsOrganizationAddress(lab.getSameAsOrganizationAddress());
        details.setLabAddressLine2(lab.getLabAddressLine2());
        details.setLabDistrict(lab.getLabDistrict());
        details.setEstablishmentRegistrationNumber(lab.getEstablishmentRegistrationNumber());
        details.setIssuingAuthority(lab.getIssuingAuthority());
        details.setIssuingDate(lab.getIssuingDate());
        details.setExpiryDate(lab.getExpiryDate());
        details.setLabPan(lab.getLabPan());
        details.setLabPanSameAsOrganization(lab.getLabPanSameAsOrganization());
        details.setLabGstin(lab.getLabGstin());
        details.setLabGstinSameAsOrganization(lab.getLabGstinSameAsOrganization());
        details.setBiomedicalWasteGenerated(lab.getBiomedicalWasteGenerated());
        details.setBiomedicalWasteAuthorizationNumber(lab.getBiomedicalWasteAuthorizationNumber());

        LabOnboardingDetailsDTO dto = new LabOnboardingDetailsDTO();
        dto.setOrganization(org);
        dto.setLab(details);
        return dto;
    }

    private List<String> splitServices(String stored) {
        List<String> services = new ArrayList<>();
        if (isBlank(stored)) {
            return services;
        }
        for (String service : stored.split(",")) {
            if (!service.isBlank()) {
                services.add(service.trim());
            }
        }
        return services;
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private Lab getAccessibleLab(User currentUser, Long labId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new NoSuchElementException("Lab not found"));
        if (!userRepository.existsByIdAndLabsId(currentUser.getId(), labId)) {
            throw new IllegalStateException("User is not a member of this lab");
        }
        return lab;
    }

    /** Null = leave unchanged; blank = not allowed; otherwise trimmed value is set. */
    private void setRequired(String label, String value, Consumer<String> setter) {
        if (value == null) {
            return;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        setter.accept(value.trim());
    }

    /** Null = leave unchanged; blank = clear the value; otherwise trimmed value is set. */
    private void setOptional(String value, Consumer<String> setter) {
        if (value == null) {
            return;
        }
        setter.accept(value.isBlank() ? null : value.trim());
    }

    private static boolean isAllowed(Set<String> allowedValues, String value) {
        return value != null && allowedValues.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(value.trim()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
