package tiameds.com.tiameds.services.onboarding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.dto.onboarding.OnboardingRequestDTO;
import tiameds.com.tiameds.dto.onboarding.OnboardingResponseDTO;
import tiameds.com.tiameds.entity.EntityType;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.LabApprovalStatus;
import tiameds.com.tiameds.entity.Role;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VerificationToken;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.RoleRepository;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.services.lab.SequenceGeneratorService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Service for handling the onboarding process: user creation and Lab creation after email verification.
 */
@Slf4j
@Service
public class OnboardingService {

    private static final Set<String> ORGANIZATION_TYPES = Set.of(
            "Proprietorship", "Partnership", "LLP", "Private Limited Company", "Public Limited Company",
            "Trust", "Society", "Government Institution", "Other");

    private static final Set<String> LAB_SERVICES = Set.of(
            "Clinical Pathology", "Biochemistry", "Haematology", "Microbiology", "Histopathology",
            "Cytopathology", "Serology", "Immunology", "Molecular Diagnostics", "Genetic Testing",
            "Ultrasound", "X-ray", "CT Scan", "MRI", "Blood Banking", "Sample Collection", "All");

    private final UserRepository userRepository;
    private final LabRepository labRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenService tokenService;
    private final SequenceGeneratorService sequenceGeneratorService;

    public OnboardingService(
            UserRepository userRepository,
            LabRepository labRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            VerificationTokenService tokenService,
            SequenceGeneratorService sequenceGeneratorService) {
        this.userRepository = userRepository;
        this.labRepository = labRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    /**
     * Completes the onboarding process:
     * 1. Validates and consumes the verification token
     * 2. Creates the user account
     * 3. Creates the Lab (organization)
     * 4. Associates user with the lab
     * 5. Activates the account
     *
     * This is all done in a single transaction to ensure atomicity.
     *
     * @param request The onboarding request containing user and lab information
     * @return OnboardingResponseDTO with created user and lab details
     */
    @Transactional
    public OnboardingResponseDTO completeOnboarding(OnboardingRequestDTO request) {
        // Validate the additional onboarding fields first so a bad request does not consume the token
        validateAdditionalFields(request);

        // Step 1: Validate and consume the token
        Optional<VerificationToken> tokenOpt = tokenService.validateAndConsumeToken(request.getToken());
        
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }

        VerificationToken token = tokenOpt.get();
        
        // Verify email matches
        if (!token.getEmail().equalsIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email does not match the verification token");
        }

        // Step 2: Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        // Step 3: Create user account
        User user = new User();
        
        // Generate unique user code using sequence generator
        // Use lab ID 0 for system-level users (users created without a lab initially)
        String userCode = sequenceGeneratorService.generateCode(0L, EntityType.USER);
        user.setUserCode(userCode);
        
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setCity(request.getCity());
        user.setState(request.getState());
        user.setZip(request.getZip());
        user.setCountry(request.getCountry());
        user.setVerified(true); // Verified via email token
        user.setEnabled(true); // Account is active after onboarding
        user.setTokenVersion(0);

        // Assign default role (you may want to customize this)
        Role defaultRole = roleRepository.findByName("SUPERADMIN")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("SUPERADMIN");
                    return roleRepository.save(newRole);
                });
        
        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        log.info("Created user account during onboarding: {}", savedUser.getUsername());

        // Step 4: Create Lab (organization)
        Lab lab = new Lab();
        OnboardingRequestDTO.LabInfoDTO labInfo = request.getLab();
        
        lab.setName(labInfo.getName());
        lab.setAddress(labInfo.getAddress());
        lab.setCity(labInfo.getCity());
        lab.setState(labInfo.getState());
        lab.setDescription(labInfo.getDescription());
        // The lab stays inactive until a platform admin approves the onboarding request
        lab.setIsActive(false);
        lab.setApprovalStatus(LabApprovalStatus.PENDING);
        lab.setApprovalRequestedAt(LocalDateTime.now());
        lab.setCreatedBy(savedUser);
        
        // Set all lab fields
        lab.setLabLogo(labInfo.getLabLogo());
        lab.setLicenseNumber(labInfo.getLicenseNumber());
        lab.setLabType(labInfo.getLabType());
        lab.setLabZip(labInfo.getLabZip());
        lab.setLabCountry(labInfo.getLabCountry());
        lab.setLabPhone(labInfo.getLabPhone());
        lab.setLabEmail(labInfo.getLabEmail());
        lab.setDirectorName(labInfo.getDirectorName());
        lab.setDirectorEmail(labInfo.getDirectorEmail());
        lab.setDirectorPhone(labInfo.getDirectorPhone());
        lab.setCertificationBody(labInfo.getCertificationBody());
        lab.setLabCertificate(labInfo.getLabCertificate());
        lab.setDirectorGovtId(labInfo.getDirectorGovtId());
        lab.setLabBusinessRegistration(labInfo.getLabBusinessRegistration());
        lab.setLabLicense(labInfo.getLabLicense());
        lab.setTaxId(labInfo.getTaxId());
        lab.setLabAccreditation(labInfo.getLabAccreditation());
        lab.setDataPrivacyAgreement(labInfo.getDataPrivacyAgreement() != null && labInfo.getDataPrivacyAgreement());

        applyAdditionalFields(lab, request);

        Lab savedLab = labRepository.save(lab);
        log.info("Created lab during onboarding: {} (ID: {})", savedLab.getName(), savedLab.getId());

        // Step 5: Add user as member of the lab
        savedLab.getMembers().add(savedUser);
        labRepository.save(savedLab);
        log.info("Added user {} as member of lab {}", savedUser.getUsername(), savedLab.getName());

        // Build response
        return OnboardingResponseDTO.builder()
                .message("Onboarding submitted successfully. Your lab is pending approval.")
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .labId(savedLab.getId())
                .labName(savedLab.getName())
                .accountActive(true)
                .labApprovalStatus(savedLab.getApprovalStatus().name())
                .build();
    }

    /**
     * Validates the optional organization / compliance / terms fields.
     * Only rules that depend on the new fields are checked, so requests without them are unaffected.
     */
    private void validateAdditionalFields(OnboardingRequestDTO request) {
        if (request.getConfirmPassword() != null && !request.getConfirmPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        OnboardingRequestDTO.OrganizationInfoDTO org = request.getOrganization();
        if (org != null && org.getType() != null && !isAllowed(ORGANIZATION_TYPES, org.getType())) {
            throw new IllegalArgumentException("Invalid organization type: " + org.getType());
        }

        OnboardingRequestDTO.LabInfoDTO labInfo = request.getLab();
        if (labInfo == null) {
            return;
        }

        if (labInfo.getLabServices() != null) {
            for (String service : labInfo.getLabServices()) {
                if (!isAllowed(LAB_SERVICES, service)) {
                    throw new IllegalArgumentException("Invalid laboratory service: " + service);
                }
            }
        }

        if (labInfo.getIssuingDate() != null && labInfo.getExpiryDate() != null
                && labInfo.getExpiryDate().isBefore(labInfo.getIssuingDate())) {
            throw new IllegalArgumentException("Expiry date cannot be before issuing date");
        }

        // Biomedical waste authorization details are conditionally mandatory
        if (Boolean.TRUE.equals(labInfo.getBiomedicalWasteGenerated())) {
            if (isBlank(labInfo.getBiomedicalWasteAuthorizationNumber())) {
                throw new IllegalArgumentException("Biomedical waste authorization number is required when biomedical waste is generated");
            }
            if (isBlank(labInfo.getBiomedicalWasteAuthorizationCertificate())) {
                throw new IllegalArgumentException("Biomedical waste authorization certificate is required when biomedical waste is generated");
            }
        }
    }

    /**
     * Copies the optional organization / compliance / terms fields onto the lab.
     */
    private void applyAdditionalFields(Lab lab, OnboardingRequestDTO request) {
        OnboardingRequestDTO.OrganizationInfoDTO org = request.getOrganization();
        OnboardingRequestDTO.LabInfoDTO labInfo = request.getLab();

        if (org != null) {
            lab.setOrganizationName(org.getName());
            lab.setOrganizationType(org.getType());
            lab.setOrganizationEmail(org.getEmail());
            lab.setOrganizationPhone(org.getPhone());
            lab.setOrganizationAddressLine1(org.getAddressLine1());
            lab.setOrganizationAddressLine2(org.getAddressLine2());
            lab.setOrganizationCountry(org.getCountry());
            lab.setOrganizationState(org.getState());
            lab.setOrganizationDistrict(org.getDistrict());
            lab.setOrganizationCity(org.getCity());
            lab.setOrganizationPinCode(org.getPinCode());
            lab.setOrganizationPan(org.getPan());
            lab.setOrganizationGstin(org.getGstin());
        }

        // Lab services (multi-select) stored as comma-separated values
        if (labInfo.getLabServices() != null && !labInfo.getLabServices().isEmpty()) {
            lab.setLabServices(String.join(",", labInfo.getLabServices()));
        }

        lab.setSameAsOrganizationAddress(labInfo.getSameAsOrganizationAddress());
        lab.setLabAddressLine2(labInfo.getLabAddressLine2());
        lab.setLabDistrict(labInfo.getLabDistrict());
        if (org != null && Boolean.TRUE.equals(labInfo.getSameAsOrganizationAddress())) {
            if (isBlank(lab.getLabAddressLine2())) {
                lab.setLabAddressLine2(org.getAddressLine2());
            }
            if (isBlank(lab.getLabDistrict())) {
                lab.setLabDistrict(org.getDistrict());
            }
        }

        lab.setEstablishmentRegistrationNumber(labInfo.getEstablishmentRegistrationNumber());
        lab.setIssuingAuthority(labInfo.getIssuingAuthority());
        lab.setIssuingDate(labInfo.getIssuingDate());
        lab.setExpiryDate(labInfo.getExpiryDate());
        lab.setEstablishmentRegistrationCertificate(labInfo.getEstablishmentRegistrationCertificate());

        lab.setLabPanSameAsOrganization(labInfo.getLabPanSameAsOrganization());
        lab.setLabPan(labInfo.getLabPan());
        if (org != null && Boolean.TRUE.equals(labInfo.getLabPanSameAsOrganization()) && isBlank(lab.getLabPan())) {
            lab.setLabPan(org.getPan());
        }

        lab.setLabGstinSameAsOrganization(labInfo.getLabGstinSameAsOrganization());
        lab.setLabGstin(labInfo.getLabGstin());
        if (org != null && Boolean.TRUE.equals(labInfo.getLabGstinSameAsOrganization()) && isBlank(lab.getLabGstin())) {
            lab.setLabGstin(org.getGstin());
        }
        lab.setGstinCertificate(labInfo.getGstinCertificate());

        lab.setBiomedicalWasteGenerated(labInfo.getBiomedicalWasteGenerated());
        lab.setBiomedicalWasteAuthorizationNumber(labInfo.getBiomedicalWasteAuthorizationNumber());
        lab.setBiomedicalWasteAuthorizationCertificate(labInfo.getBiomedicalWasteAuthorizationCertificate());
        lab.setNablAccreditationCertificate(labInfo.getNablAccreditationCertificate());

        // Authorization and Terms & Conditions acceptance record (version + timestamp)
        lab.setRegistrationAuthorizationAccepted(request.getRegistrationAuthorizationAccepted());
        lab.setTermsAccepted(request.getTermsAccepted());
        if (Boolean.TRUE.equals(request.getTermsAccepted())) {
            lab.setTermsVersion(request.getTermsVersion());
            lab.setTermsAcceptedAt(LocalDateTime.now());
        }
    }

    private static boolean isAllowed(Set<String> allowedValues, String value) {
        return value != null && allowedValues.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(value.trim()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

