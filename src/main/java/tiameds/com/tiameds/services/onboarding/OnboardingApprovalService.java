package tiameds.com.tiameds.services.onboarding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tiameds.com.tiameds.dto.onboarding.OnboardingRequestSummaryDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.LabApprovalStatus;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.services.email.EmailService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Review of onboarding requests: a lab registered through onboarding stays inactive
 * until a platform admin approves it.
 */
@Slf4j
@Service
public class OnboardingApprovalService {

    private final LabRepository labRepository;
    private final EmailService emailService;

    public OnboardingApprovalService(LabRepository labRepository, EmailService emailService) {
        this.labRepository = labRepository;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<OnboardingRequestSummaryDTO> getRequests(LabApprovalStatus status) {
        return labRepository.findByApprovalStatusOrderByApprovalRequestedAtAsc(status).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public OnboardingRequestSummaryDTO approve(Long labId) {
        Lab lab = getPendingLab(labId);
        lab.setApprovalStatus(LabApprovalStatus.APPROVED);
        lab.setIsActive(true);
        lab.setRejectionReason(null);
        lab.setReviewedAt(LocalDateTime.now());
        Lab saved = labRepository.save(lab);
        log.info("Onboarding request for lab {} approved", saved.getId());
        sendApprovalEmailAfterCommit(saved.getCreatedBy());
        return toSummary(saved);
    }

    /**
     * Notifies the applicant on their verified email once the approval is committed.
     * A mail failure is logged and never undoes the approval.
     */
    private void sendApprovalEmailAfterCommit(User applicant) {
        if (applicant == null || applicant.getEmail() == null || applicant.getEmail().isBlank()) {
            log.warn("No applicant email found; approval email not sent");
            return;
        }
        String email = applicant.getEmail();
        // Sent off the request thread so a slow SMTP server never delays the admin's approve call
        Runnable send = () -> CompletableFuture.runAsync(() -> {
            try {
                emailService.sendApplicationApprovedEmail(email);
            } catch (Exception e) {
                log.error("Approval email could not be sent to {}: {}", email, e.getMessage());
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    @Transactional
    public OnboardingRequestSummaryDTO reject(Long labId, String reason) {
        Lab lab = getPendingLab(labId);
        lab.setApprovalStatus(LabApprovalStatus.REJECTED);
        lab.setIsActive(false);
        lab.setRejectionReason(reason.trim());
        lab.setReviewedAt(LocalDateTime.now());
        Lab saved = labRepository.save(lab);
        log.info("Onboarding request for lab {} rejected", saved.getId());
        return toSummary(saved);
    }

    /**
     * Login gate for onboarded labs. A user is blocked only when they belong to labs and none of them is approved,
     * so users without labs and users of pre-existing (APPROVED) labs are unaffected.
     *
     * @return the message to show instead of logging in, or empty if the user may log in
     */
    @Transactional(readOnly = true)
    public Optional<String> getLoginBlockedMessage(Long userId) {
        List<LabApprovalStatus> statuses = labRepository.findApprovalStatusesByMemberId(userId);
        if (statuses.isEmpty() || statuses.contains(LabApprovalStatus.APPROVED)) {
            return Optional.empty();
        }
        if (statuses.contains(LabApprovalStatus.PENDING)) {
            return Optional.of("Your application is under review. Please wait until it is approved.");
        }
        return Optional.of("Your application was not approved. Please contact support.");
    }

    private Lab getPendingLab(Long labId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new NoSuchElementException("Lab not found"));
        if (lab.getApprovalStatus() != LabApprovalStatus.PENDING) {
            throw new IllegalStateException("Onboarding request is not pending (current status: "
                    + lab.getApprovalStatus() + ")");
        }
        return lab;
    }

    private OnboardingRequestSummaryDTO toSummary(Lab lab) {
        User applicant = lab.getCreatedBy();
        return OnboardingRequestSummaryDTO.builder()
                .labId(lab.getId())
                .labName(lab.getName())
                .labType(lab.getLabType())
                .labEmail(lab.getLabEmail())
                .labPhone(lab.getLabPhone())
                .city(lab.getCity())
                .state(lab.getState())
                .labServices(lab.getLabServices())
                .organizationName(lab.getOrganizationName())
                .organizationType(lab.getOrganizationType())
                .organizationPan(lab.getOrganizationPan())
                .organizationGstin(lab.getOrganizationGstin())
                .licenseNumber(lab.getLicenseNumber())
                .establishmentRegistrationNumber(lab.getEstablishmentRegistrationNumber())
                .issuingAuthority(lab.getIssuingAuthority())
                .issuingDate(lab.getIssuingDate())
                .expiryDate(lab.getExpiryDate())
                .labLogo(lab.getLabLogo())
                .establishmentRegistrationCertificate(lab.getEstablishmentRegistrationCertificate())
                .organizationGstinCertificate(lab.getOrganizationGstinCertificate())
                .gstinCertificate(lab.getGstinCertificate())
                .biomedicalWasteAuthorizationCertificate(lab.getBiomedicalWasteAuthorizationCertificate())
                .nablAccreditationCertificate(lab.getNablAccreditationCertificate())
                .applicantUserId(applicant != null ? applicant.getId() : null)
                .applicantName(applicant != null ? applicant.getFullName() : null)
                .applicantEmail(applicant != null ? applicant.getEmail() : null)
                .approvalStatus(lab.getApprovalStatus().name())
                .approvalRequestedAt(lab.getApprovalRequestedAt())
                .reviewedAt(lab.getReviewedAt())
                .reviewedBy(lab.getReviewedBy())
                .rejectionReason(lab.getRejectionReason())
                .build();
    }
}
