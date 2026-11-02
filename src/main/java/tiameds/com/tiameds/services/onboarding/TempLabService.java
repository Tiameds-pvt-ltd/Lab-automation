package tiameds.com.tiameds.services.onboarding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tiameds.com.tiameds.dto.onboarding.OnboardingRequestDTO;
import tiameds.com.tiameds.dto.onboarding.OnboardingRequestSummaryDTO;
import tiameds.com.tiameds.dto.onboarding.OnboardingResponseDTO;
import tiameds.com.tiameds.entity.EntityType;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.LabApprovalStatus;
import tiameds.com.tiameds.entity.Role;
import tiameds.com.tiameds.entity.TempLab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VerificationToken;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.RoleRepository;
import tiameds.com.tiameds.repository.TempLabRepository;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.services.S3StorageService;
import tiameds.com.tiameds.services.email.EmailService;
import tiameds.com.tiameds.services.lab.LabDefaultDataService;
import tiameds.com.tiameds.services.lab.LabDocumentService;
import tiameds.com.tiameds.services.lab.SequenceGeneratorService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Onboarding through the temp_lab staging table.
 * <ul>
 *   <li>submit: validates the form and stores everything in temp_lab; no user or lab exists yet</li>
 *   <li>approve: creates the user and the (active) lab from the temp_lab row, the same way onboarding did before</li>
 *   <li>reject: marks the row REJECTED; nothing is ever created in users / labs</li>
 * </ul>
 */
@Slf4j
@Service
public class TempLabService {

    private static final Set<String> ORGANIZATION_TYPES = Set.of(
            "Proprietorship", "Partnership", "LLP", "Private Limited Company", "Public Limited Company",
            "Trust", "Society", "Government Institution", "Other");

    private static final Set<String> LAB_SERVICES = Set.of(
            "Clinical Pathology", "Biochemistry", "Haematology", "Microbiology", "Histopathology",
            "Cytopathology", "Serology", "Immunology", "Molecular Diagnostics", "Genetic Testing",
            "Ultrasound", "X-ray", "CT Scan", "MRI", "Blood Banking", "Sample Collection", "All");

    private final TempLabRepository tempLabRepository;
    private final UserRepository userRepository;
    private final LabRepository labRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenService tokenService;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final S3StorageService s3StorageService;
    private final EmailService emailService;
    private final LabDefaultDataService labDefaultDataService;

    public TempLabService(
            TempLabRepository tempLabRepository,
            UserRepository userRepository,
            LabRepository labRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            VerificationTokenService tokenService,
            SequenceGeneratorService sequenceGeneratorService,
            S3StorageService s3StorageService,
            EmailService emailService,
            LabDefaultDataService labDefaultDataService) {
        this.tempLabRepository = tempLabRepository;
        this.userRepository = userRepository;
        this.labRepository = labRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.s3StorageService = s3StorageService;
        this.emailService = emailService;
        this.labDefaultDataService = labDefaultDataService;
    }

    // ------------------------------------------------------------------ submit

    /**
     * Stores an onboarding submission in temp_lab. The verification token is consumed here (single-use);
     * a failure anywhere rolls the whole transaction back, so the token is not lost.
     */
    @Transactional
    public OnboardingResponseDTO submit(OnboardingRequestDTO request) {
        validateAdditionalFields(request);

        Optional<VerificationToken> tokenOpt = tokenService.validateAndConsumeToken(request.getToken());
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired verification token");
        }
        if (!tokenOpt.get().getEmail().equalsIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email does not match the verification token");
        }

        if (userRepository.existsByEmail(request.getEmail())
                || tempLabRepository.existsByEmailIgnoreCaseAndApprovalStatus(request.getEmail(), LabApprovalStatus.PENDING)) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())
                || tempLabRepository.existsByUsernameAndApprovalStatus(request.getUsername(), LabApprovalStatus.PENDING)) {
            throw new IllegalArgumentException("Username is already taken");
        }

        TempLab saved = tempLabRepository.save(toTempLab(request));
        log.info("Stored onboarding request in temp_lab: {} (ID: {})", saved.getLabName(), saved.getId());

        return OnboardingResponseDTO.builder()
                .message("Onboarding submitted successfully. Your lab is pending approval.")
                .username(saved.getUsername())
                .email(saved.getEmail())
                .labName(saved.getLabName())
                .accountActive(false)
                .labApprovalStatus(saved.getApprovalStatus().name())
                .build();
    }

    private TempLab toTempLab(OnboardingRequestDTO request) {
        TempLab temp = new TempLab();

        temp.setEmail(request.getEmail());
        temp.setUsername(request.getUsername());
        temp.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        temp.setFirstName(request.getFirstName());
        temp.setLastName(request.getLastName());
        temp.setPhone(request.getPhone());
        temp.setAddress(request.getAddress());
        temp.setCity(request.getCity());
        temp.setState(request.getState());
        temp.setZip(request.getZip());
        temp.setCountry(request.getCountry());

        OnboardingRequestDTO.OrganizationInfoDTO org = request.getOrganization();
        if (org != null) {
            temp.setOrganizationName(org.getName());
            temp.setOrganizationType(org.getType());
            temp.setOrganizationEmail(org.getEmail());
            temp.setOrganizationPhone(org.getPhone());
            temp.setOrganizationAddressLine1(org.getAddressLine1());
            temp.setOrganizationAddressLine2(org.getAddressLine2());
            temp.setOrganizationCountry(org.getCountry());
            temp.setOrganizationState(org.getState());
            temp.setOrganizationDistrict(org.getDistrict());
            temp.setOrganizationCity(org.getCity());
            temp.setOrganizationPinCode(org.getPinCode());
            temp.setOrganizationPan(org.getPan());
            temp.setOrganizationGstin(org.getGstin());
            temp.setOrganizationGstinCertificate(org.getGstinCertificate());
        }

        OnboardingRequestDTO.LabInfoDTO labInfo = request.getLab();
        temp.setLabName(labInfo.getName());
        temp.setLabAddress(labInfo.getAddress());
        temp.setLabCity(labInfo.getCity());
        temp.setLabState(labInfo.getState());
        temp.setLabDescription(labInfo.getDescription());
        temp.setLabLogo(labInfo.getLabLogo());
        temp.setLicenseNumber(labInfo.getLicenseNumber());
        temp.setLabType(labInfo.getLabType());
        temp.setLabZip(labInfo.getLabZip());
        temp.setLabCountry(labInfo.getLabCountry());
        temp.setLabPhone(labInfo.getLabPhone());
        temp.setLabEmail(labInfo.getLabEmail());
        temp.setDirectorName(labInfo.getDirectorName());
        temp.setDirectorEmail(labInfo.getDirectorEmail());
        temp.setDirectorPhone(labInfo.getDirectorPhone());
        temp.setCertificationBody(labInfo.getCertificationBody());
        temp.setLabCertificate(labInfo.getLabCertificate());
        temp.setDirectorGovtId(labInfo.getDirectorGovtId());
        temp.setLabBusinessRegistration(labInfo.getLabBusinessRegistration());
        temp.setLabLicense(labInfo.getLabLicense());
        temp.setTaxId(labInfo.getTaxId());
        temp.setLabAccreditation(labInfo.getLabAccreditation());
        temp.setDataPrivacyAgreement(Boolean.TRUE.equals(labInfo.getDataPrivacyAgreement()));

        // Lab services (multi-select) stored as comma-separated values
        if (labInfo.getLabServices() != null && !labInfo.getLabServices().isEmpty()) {
            temp.setLabServices(String.join(",", labInfo.getLabServices()));
        }
        temp.setSameAsOrganizationAddress(labInfo.getSameAsOrganizationAddress());
        temp.setLabAddressLine2(labInfo.getLabAddressLine2());
        temp.setLabDistrict(labInfo.getLabDistrict());

        temp.setEstablishmentRegistrationNumber(labInfo.getEstablishmentRegistrationNumber());
        temp.setIssuingAuthority(labInfo.getIssuingAuthority());
        temp.setIssuingDate(labInfo.getIssuingDate());
        temp.setExpiryDate(labInfo.getExpiryDate());
        temp.setEstablishmentRegistrationCertificate(labInfo.getEstablishmentRegistrationCertificate());
        temp.setLabPan(labInfo.getLabPan());
        temp.setLabPanSameAsOrganization(labInfo.getLabPanSameAsOrganization());
        temp.setLabGstin(labInfo.getLabGstin());
        temp.setLabGstinSameAsOrganization(labInfo.getLabGstinSameAsOrganization());
        temp.setGstinCertificate(labInfo.getGstinCertificate());
        temp.setBiomedicalWasteGenerated(labInfo.getBiomedicalWasteGenerated());
        temp.setBiomedicalWasteAuthorizationNumber(labInfo.getBiomedicalWasteAuthorizationNumber());
        temp.setBiomedicalWasteAuthorizationCertificate(labInfo.getBiomedicalWasteAuthorizationCertificate());
        temp.setNablAccreditationCertificate(labInfo.getNablAccreditationCertificate());

        // Authorization and Terms & Conditions acceptance record (version + timestamp)
        temp.setRegistrationAuthorizationAccepted(request.getRegistrationAuthorizationAccepted());
        temp.setTermsAccepted(request.getTermsAccepted());
        if (Boolean.TRUE.equals(request.getTermsAccepted())) {
            temp.setTermsVersion(request.getTermsVersion());
            temp.setTermsAcceptedAt(LocalDateTime.now());
        }

        temp.setApprovalStatus(LabApprovalStatus.PENDING);
        temp.setApprovalRequestedAt(LocalDateTime.now());
        return temp;
    }

    // ------------------------------------------------------------------ login gate

    /**
     * Explains a failed login to an applicant whose onboarding request is still in temp_lab (no user exists yet).
     * The answer is given only when the password matches the one submitted with the request, so the existence of an
     * application is never revealed to someone who merely knows a username or email.
     *
     * @return the message to show instead of "Incorrect username or password", or empty for any other failed login
     */
    @Transactional(readOnly = true)
    public Optional<String> getLoginBlockedMessage(String usernameOrEmail, String rawPassword) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || rawPassword == null) {
            return Optional.empty();
        }
        try {
            // A real account always wins; its failed login is an ordinary wrong password
            if (userRepository.existsByUsername(usernameOrEmail) || userRepository.existsByEmail(usernameOrEmail)) {
                return Optional.empty();
            }

            Optional<TempLab> latest = tempLabRepository
                    .findFirstByUsernameOrEmailIgnoreCaseOrderByApprovalRequestedAtDesc(usernameOrEmail, usernameOrEmail);
            if (latest.isEmpty() || !passwordEncoder.matches(rawPassword, latest.get().getPasswordHash())) {
                return Optional.empty();
            }

            TempLab temp = latest.get();
            if (temp.getApprovalStatus() == LabApprovalStatus.PENDING) {
                return Optional.of("Your application is not yet approved. You can log in once it is approved.");
            }
            if (temp.getApprovalStatus() == LabApprovalStatus.REJECTED) {
                String reason = temp.getRejectionReason();
                return Optional.of(reason == null || reason.isBlank()
                        ? "Your application was not approved. Please contact support."
                        : "Your application was not approved. Reason: " + reason.trim());
            }
            return Optional.empty();
        } catch (RuntimeException ex) {
            // Never let this extra lookup turn a normal "incorrect credentials" answer into a server error
            log.warn("Could not check onboarding status at login: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------ review

    @Transactional(readOnly = true)
    public List<OnboardingRequestSummaryDTO> getRequests(LabApprovalStatus status) {
        return tempLabRepository.findByApprovalStatusOrderByApprovalRequestedAtAsc(status).stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * The add-lab requests of one user (pending, approved and rejected with the reason), newest first.
     */
    @Transactional(readOnly = true)
    public List<OnboardingRequestSummaryDTO> getRequestsOfUser(Long userId) {
        return tempLabRepository.findByExistingUserIdOrderByApprovalRequestedAtDesc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * Creates the active lab from the temp_lab row, and the user too for an onboarding request
     * (an add-lab request already has its user). Nothing is created if any step fails.
     */
    @Transactional
    public OnboardingRequestSummaryDTO approve(Long tempLabId) {
        TempLab temp = getPending(tempLabId);

        User savedUser;
        if (temp.getExistingUserId() != null) {
            // add-lab request: the user already exists, only the lab is created for them
            savedUser = userRepository.findById(temp.getExistingUserId())
                    .orElseThrow(() -> new IllegalStateException("The user who requested this lab no longer exists"));
            if (labRepository.existsByName(temp.getLabName())) {
                throw new IllegalStateException("A lab with this name already exists");
            }
        } else {
            // The applicant could have been created through another route since the request was submitted
            if (userRepository.existsByEmail(temp.getEmail())) {
                throw new IllegalStateException("A user with this email already exists");
            }
            if (userRepository.existsByUsername(temp.getUsername())) {
                throw new IllegalStateException("A user with this username already exists");
            }

            savedUser = userRepository.save(toUser(temp));
            log.info("Created user account for approved onboarding request {}: {}", temp.getId(), savedUser.getUsername());
        }

        Lab lab = toLab(temp, savedUser);
        lab.getMembers().add(savedUser);
        Lab savedLab = labRepository.save(lab);
        log.info("Created lab for approved onboarding request {}: {} (ID: {})", temp.getId(), savedLab.getName(), savedLab.getId());

        temp.setApprovalStatus(LabApprovalStatus.APPROVED);
        temp.setRejectionReason(null);
        temp.setReviewedAt(LocalDateTime.now());
        temp.setApprovedUserId(savedUser.getId());
        temp.setApprovedLabId(savedLab.getId());
        TempLab saved = tempLabRepository.save(temp);

        afterCommit(() -> notifyAndSeed(savedUser.getEmail(), savedLab.getId(), savedUser.getId()));
        return toSummary(saved);
    }

    /**
     * Rejects the request. The row stays in temp_lab only; no user or lab is created.
     */
    @Transactional
    public OnboardingRequestSummaryDTO reject(Long tempLabId, String reason) {
        TempLab temp = getPending(tempLabId);
        temp.setApprovalStatus(LabApprovalStatus.REJECTED);
        temp.setRejectionReason(reason.trim());
        temp.setReviewedAt(LocalDateTime.now());
        TempLab saved = tempLabRepository.save(temp);
        log.info("Onboarding request {} rejected", saved.getId());
        return toSummary(saved);
    }

    private TempLab getPending(Long tempLabId) {
        TempLab temp = tempLabRepository.findById(tempLabId)
                .orElseThrow(() -> new NoSuchElementException("Onboarding request not found"));
        if (temp.getApprovalStatus() != LabApprovalStatus.PENDING) {
            throw new IllegalStateException("Onboarding request is not pending (current status: "
                    + temp.getApprovalStatus() + ")");
        }
        return temp;
    }

    private User toUser(TempLab temp) {
        User user = new User();

        // Use lab ID 0 for system-level users (users created without a lab initially)
        user.setUserCode(sequenceGeneratorService.generateCode(0L, EntityType.USER));
        user.setUsername(temp.getUsername());
        user.setPassword(temp.getPasswordHash());
        user.setEmail(temp.getEmail());
        user.setFirstName(temp.getFirstName());
        user.setLastName(temp.getLastName());
        user.setPhone(temp.getPhone());
        user.setAddress(temp.getAddress());
        user.setCity(temp.getCity());
        user.setState(temp.getState());
        user.setZip(temp.getZip());
        user.setCountry(temp.getCountry());
        user.setVerified(true); // Verified via email token at submission
        user.setEnabled(true);
        user.setTokenVersion(0);

        Role defaultRole = roleRepository.findByName("SUPERADMIN")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("SUPERADMIN");
                    return roleRepository.save(newRole);
                });
        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);
        return user;
    }

    private Lab toLab(TempLab temp, User createdBy) {
        Lab lab = new Lab();

        lab.setName(temp.getLabName());
        lab.setAddress(temp.getLabAddress());
        lab.setCity(temp.getLabCity());
        lab.setState(temp.getLabState());
        lab.setDescription(temp.getLabDescription());
        lab.setIsActive(true);
        lab.setApprovalStatus(LabApprovalStatus.APPROVED);
        lab.setApprovalRequestedAt(temp.getApprovalRequestedAt());
        lab.setReviewedAt(temp.getReviewedAt() != null ? temp.getReviewedAt() : LocalDateTime.now());
        lab.setCreatedBy(createdBy);

        lab.setLabLogo(temp.getLabLogo());
        lab.setLicenseNumber(temp.getLicenseNumber());
        lab.setLabType(temp.getLabType());
        lab.setLabZip(temp.getLabZip());
        lab.setLabCountry(temp.getLabCountry());
        lab.setLabPhone(temp.getLabPhone());
        lab.setLabEmail(temp.getLabEmail());
        lab.setDirectorName(temp.getDirectorName());
        lab.setDirectorEmail(temp.getDirectorEmail());
        lab.setDirectorPhone(temp.getDirectorPhone());
        lab.setCertificationBody(temp.getCertificationBody());
        lab.setLabCertificate(temp.getLabCertificate());
        lab.setDirectorGovtId(temp.getDirectorGovtId());
        lab.setLabBusinessRegistration(temp.getLabBusinessRegistration());
        lab.setLabLicense(temp.getLabLicense());
        lab.setTaxId(temp.getTaxId());
        lab.setLabAccreditation(temp.getLabAccreditation());
        lab.setDataPrivacyAgreement(temp.getDataPrivacyAgreement());

        lab.setOrganizationName(temp.getOrganizationName());
        lab.setOrganizationType(temp.getOrganizationType());
        lab.setOrganizationEmail(temp.getOrganizationEmail());
        lab.setOrganizationPhone(temp.getOrganizationPhone());
        lab.setOrganizationAddressLine1(temp.getOrganizationAddressLine1());
        lab.setOrganizationAddressLine2(temp.getOrganizationAddressLine2());
        lab.setOrganizationCountry(temp.getOrganizationCountry());
        lab.setOrganizationState(temp.getOrganizationState());
        lab.setOrganizationDistrict(temp.getOrganizationDistrict());
        lab.setOrganizationCity(temp.getOrganizationCity());
        lab.setOrganizationPinCode(temp.getOrganizationPinCode());
        lab.setOrganizationPan(temp.getOrganizationPan());
        lab.setOrganizationGstin(temp.getOrganizationGstin());
        lab.setOrganizationGstinCertificate(temp.getOrganizationGstinCertificate());

        lab.setLabServices(temp.getLabServices());

        // "Same as organization" fields fall back to the organization value when the lab value is blank
        lab.setSameAsOrganizationAddress(temp.getSameAsOrganizationAddress());
        lab.setLabAddressLine2(temp.getLabAddressLine2());
        lab.setLabDistrict(temp.getLabDistrict());
        if (Boolean.TRUE.equals(temp.getSameAsOrganizationAddress())) {
            if (isBlank(lab.getLabAddressLine2())) {
                lab.setLabAddressLine2(temp.getOrganizationAddressLine2());
            }
            if (isBlank(lab.getLabDistrict())) {
                lab.setLabDistrict(temp.getOrganizationDistrict());
            }
        }

        lab.setEstablishmentRegistrationNumber(temp.getEstablishmentRegistrationNumber());
        lab.setIssuingAuthority(temp.getIssuingAuthority());
        lab.setIssuingDate(temp.getIssuingDate());
        lab.setExpiryDate(temp.getExpiryDate());
        lab.setEstablishmentRegistrationCertificate(temp.getEstablishmentRegistrationCertificate());

        lab.setLabPanSameAsOrganization(temp.getLabPanSameAsOrganization());
        lab.setLabPan(temp.getLabPan());
        if (Boolean.TRUE.equals(temp.getLabPanSameAsOrganization()) && isBlank(lab.getLabPan())) {
            lab.setLabPan(temp.getOrganizationPan());
        }

        lab.setLabGstinSameAsOrganization(temp.getLabGstinSameAsOrganization());
        lab.setLabGstin(temp.getLabGstin());
        if (Boolean.TRUE.equals(temp.getLabGstinSameAsOrganization()) && isBlank(lab.getLabGstin())) {
            lab.setLabGstin(temp.getOrganizationGstin());
        }
        lab.setGstinCertificate(temp.getGstinCertificate());

        lab.setBiomedicalWasteGenerated(temp.getBiomedicalWasteGenerated());
        lab.setBiomedicalWasteAuthorizationNumber(temp.getBiomedicalWasteAuthorizationNumber());
        lab.setBiomedicalWasteAuthorizationCertificate(temp.getBiomedicalWasteAuthorizationCertificate());
        lab.setNablAccreditationCertificate(temp.getNablAccreditationCertificate());

        lab.setRegistrationAuthorizationAccepted(temp.getRegistrationAuthorizationAccepted());
        lab.setTermsAccepted(temp.getTermsAccepted());
        lab.setTermsVersion(temp.getTermsVersion());
        lab.setTermsAcceptedAt(temp.getTermsAcceptedAt());
        return lab;
    }

    /**
     * Approval email and default lab data, run off the request thread once the approval is committed.
     * A failure here is logged and never undoes the approval.
     */
    private void notifyAndSeed(String applicantEmail, Long labId, Long userId) {
        CompletableFuture.runAsync(() -> {
            if (applicantEmail == null || applicantEmail.isBlank()) {
                log.warn("No applicant email found; approval email not sent");
            } else {
                try {
                    emailService.sendApplicationApprovedEmail(applicantEmail);
                } catch (Exception e) {
                    log.error("Approval email could not be sent to {}: {}", applicantEmail, e.getMessage());
                }
            }
            try {
                log.info("Starting async default data upload for lab {} (userId: {})", labId, userId);
                labDefaultDataService.uploadDefaultData(labId, userId);
                log.info("Completed async default data upload for lab {}", labId);
            } catch (Exception e) {
                log.error("Default data upload failed but approval succeeded for lab {}: {}", labId, e.getMessage(), e);
            }
        });
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    // labId carries the temp_lab id: it is the id the review endpoints take
    private OnboardingRequestSummaryDTO toSummary(TempLab temp) {
        return OnboardingRequestSummaryDTO.builder()
                .labId(temp.getId())
                .labName(temp.getLabName())
                .labType(temp.getLabType())
                .labEmail(temp.getLabEmail())
                .labPhone(temp.getLabPhone())
                .city(temp.getLabCity())
                .state(temp.getLabState())
                .labServices(temp.getLabServices())
                .organizationName(temp.getOrganizationName())
                .organizationType(temp.getOrganizationType())
                .organizationPan(temp.getOrganizationPan())
                .organizationGstin(temp.getOrganizationGstin())
                .licenseNumber(temp.getLicenseNumber())
                .establishmentRegistrationNumber(temp.getEstablishmentRegistrationNumber())
                .issuingAuthority(temp.getIssuingAuthority())
                .issuingDate(temp.getIssuingDate())
                .expiryDate(temp.getExpiryDate())
                .labLogo(temp.getLabLogo())
                .establishmentRegistrationCertificate(temp.getEstablishmentRegistrationCertificate())
                .organizationGstinCertificate(temp.getOrganizationGstinCertificate())
                .gstinCertificate(temp.getGstinCertificate())
                .biomedicalWasteAuthorizationCertificate(temp.getBiomedicalWasteAuthorizationCertificate())
                .nablAccreditationCertificate(temp.getNablAccreditationCertificate())
                .applicantUserId(temp.getApprovedUserId() != null ? temp.getApprovedUserId() : temp.getExistingUserId())
                .applicantName(((temp.getFirstName() == null ? "" : temp.getFirstName()) + " "
                        + (temp.getLastName() == null ? "" : temp.getLastName())).trim())
                .applicantEmail(temp.getEmail())
                .approvalStatus(temp.getApprovalStatus().name())
                .requestType(temp.getExistingUserId() != null ? "ADD_LAB" : "ONBOARDING")
                .approvalRequestedAt(temp.getApprovalRequestedAt())
                .reviewedAt(temp.getReviewedAt())
                .rejectionReason(temp.getRejectionReason())
                .build();
    }

    // ------------------------------------------------------------------ validation

    /**
     * Same rules OnboardingService applies to the optional organization / compliance / terms fields,
     * plus the lab accreditation number, which the lab table requires.
     */
    private void validateAdditionalFields(OnboardingRequestDTO request) {
        if (request.getConfirmPassword() != null && !request.getConfirmPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        OnboardingRequestDTO.OrganizationInfoDTO org = request.getOrganization();
        if (org != null && org.getType() != null && !isAllowed(ORGANIZATION_TYPES, org.getType())) {
            throw new IllegalArgumentException("Invalid organization type: " + org.getType());
        }

        // Documents must be files uploaded through /public/onboarding/documents, not arbitrary URLs
        if (org != null) {
            requireUploadedDocument("organizationGstinCertificate", org.getGstinCertificate());
        }

        OnboardingRequestDTO.LabInfoDTO labInfo = request.getLab();
        if (labInfo == null) {
            return;
        }

        if (isBlank(labInfo.getLabAccreditation())) {
            throw new IllegalArgumentException("Lab accreditation is required");
        }

        requireUploadedDocument("labLogo", labInfo.getLabLogo());
        requireUploadedDocument("gstinCertificate", labInfo.getGstinCertificate());
        requireUploadedDocument("establishmentRegistrationCertificate", labInfo.getEstablishmentRegistrationCertificate());
        requireUploadedDocument("biomedicalWasteAuthorizationCertificate", labInfo.getBiomedicalWasteAuthorizationCertificate());
        requireUploadedDocument("nablAccreditationCertificate", labInfo.getNablAccreditationCertificate());

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

    private void requireUploadedDocument(String field, String url) {
        if (!isBlank(url) && !s3StorageService.isUrlUnderPrefix(url, LabDocumentService.ONBOARDING_S3_PREFIX)) {
            throw new IllegalArgumentException(field + " must be a file uploaded through the onboarding document upload");
        }
    }

    private static boolean isAllowed(Set<String> allowedValues, String value) {
        return value != null && allowedValues.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(value.trim()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
