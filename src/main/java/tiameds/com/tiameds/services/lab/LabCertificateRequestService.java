package tiameds.com.tiameds.services.lab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.dto.lab.LabCertificateRequestDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.LabApprovalStatus;
import tiameds.com.tiameds.entity.LabCertificateRequest;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabCertificateRequestRepository;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.services.S3StorageService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Add / replace of lab certificates through platform-admin approval.
 * <ul>
 *   <li>submit: the file is uploaded to S3 and a PENDING request is stored; the lab is not changed</li>
 *   <li>approve: the lab points at the new file (and, for the establishment certificate, the new dates)</li>
 *   <li>reject: only the status and reason are stored; the lab is not changed</li>
 * </ul>
 * Nothing is deleted from S3 on approve or reject, the previous certificate and a rejected upload both stay as
 * history. The only deletion is of a file uploaded by a submit that itself failed and was rolled back.
 */
@Slf4j
@Service
public class LabCertificateRequestService {

    private final LabCertificateRequestRepository requestRepository;
    private final LabRepository labRepository;
    private final UserRepository userRepository;
    private final LabDocumentService labDocumentService;
    private final S3StorageService s3StorageService;

    public LabCertificateRequestService(LabCertificateRequestRepository requestRepository,
                                        LabRepository labRepository,
                                        UserRepository userRepository,
                                        LabDocumentService labDocumentService,
                                        S3StorageService s3StorageService) {
        this.requestRepository = requestRepository;
        this.labRepository = labRepository;
        this.userRepository = userRepository;
        this.labDocumentService = labDocumentService;
        this.s3StorageService = s3StorageService;
    }

    // ------------------------------------------------------------------ lab user

    /**
     * Stores a certificate request for the lab.
     *
     * @param issuingDate optional, establishment registration certificate only
     * @param expiryDate  optional, establishment registration certificate only
     * @throws IllegalArgumentException if a rule is violated (maps to 400)
     * @throws NoSuchElementException   if the lab does not exist (maps to 404)
     * @throws IllegalStateException    if the user is not a member of the lab
     */
    @Transactional
    public LabCertificateRequestDTO submit(User currentUser, Long labId, String typeParam, MultipartFile file,
                                           LocalDate issuingDate, LocalDate expiryDate) {
        Lab lab = getAccessibleLab(currentUser, labId);

        LabDocumentType type = LabDocumentType.fromParam(typeParam);
        if (type == LabDocumentType.LAB_LOGO) {
            throw new IllegalArgumentException("labLogo is not a certificate. Use the lab documents endpoint for the logo");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Certificate file is required");
        }
        boolean hasDates = issuingDate != null || expiryDate != null;
        if (hasDates && type != LabDocumentType.ESTABLISHMENT_REGISTRATION_CERTIFICATE) {
            throw new IllegalArgumentException("Issuing and expiry dates apply only to "
                    + LabDocumentType.ESTABLISHMENT_REGISTRATION_CERTIFICATE.getParam());
        }
        validateDates(issuingDate != null ? issuingDate : lab.getIssuingDate(),
                expiryDate != null ? expiryDate : lab.getExpiryDate());

        if (requestRepository.existsByLabIdAndDocumentTypeAndStatus(labId, type.name(), LabApprovalStatus.PENDING)) {
            throw new IllegalArgumentException("A request for " + type.getParam() + " is already pending review");
        }

        // Upload (validates type / size / content), kept under its own prefix so it never collides with live documents
        String newUrl = labDocumentService
                .uploadFiles("labs/" + lab.getId() + "/certificate-requests/", Map.of(type, file))
                .get(type.getParam());
        deleteUploadIfNotCommitted(newUrl);

        LabCertificateRequest request = new LabCertificateRequest();
        request.setLabId(lab.getId());
        request.setDocumentType(type.name());
        request.setRequestedByUserId(currentUser.getId());
        request.setNewFileUrl(newUrl);
        request.setIssuingDate(issuingDate);
        request.setExpiryDate(expiryDate);
        request.setStatus(LabApprovalStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());

        LabCertificateRequest saved;
        try {
            saved = requestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException e) {
            // Two submits for the same certificate raced; the partial unique index allows only one PENDING row
            throw new IllegalArgumentException("A request for " + type.getParam() + " is already pending review");
        }
        log.info("Certificate request {} ({}) submitted for lab {} by user {}",
                saved.getId(), type.getParam(), lab.getId(), currentUser.getUsername());
        return toDto(saved, lab.getName(), currentUser.getUsername());
    }

    /**
     * All requests of the lab, newest first: pending ones, rejected ones with their reason, and approved ones
     * (each carrying the file it replaced) so earlier certificates stay visible.
     */
    @Transactional(readOnly = true)
    public List<LabCertificateRequestDTO> getLabRequests(User currentUser, Long labId) {
        Lab lab = getAccessibleLab(currentUser, labId);
        return toDtos(requestRepository.findByLabIdOrderByRequestedAtDesc(labId), Map.of(lab.getId(), lab.getName()));
    }

    // ------------------------------------------------------------------ platform admin

    @Transactional(readOnly = true)
    public List<LabCertificateRequestDTO> getRequests(LabApprovalStatus status) {
        List<LabCertificateRequest> requests = requestRepository.findByStatusOrderByRequestedAtAsc(status);
        Set<Long> labIds = requests.stream().map(LabCertificateRequest::getLabId).collect(Collectors.toSet());
        Map<Long, String> labNames = new HashMap<>();
        labRepository.findAllById(labIds).forEach(lab -> labNames.put(lab.getId(), lab.getName()));
        return toDtos(requests, labNames);
    }

    /**
     * Makes the new file the lab's live certificate. The replaced file is only recorded, never deleted.
     *
     * @throws NoSuchElementException if the request or lab does not exist (maps to 404)
     * @throws IllegalStateException  if the request is not pending or the dates are inconsistent (maps to 409)
     */
    @Transactional
    public LabCertificateRequestDTO approve(Long requestId) {
        LabCertificateRequest request = getPendingForUpdate(requestId);
        Lab lab = labRepository.findById(request.getLabId())
                .orElseThrow(() -> new NoSuchElementException("Lab not found"));
        LabDocumentType type = LabDocumentType.valueOf(request.getDocumentType());

        LocalDate issuing = request.getIssuingDate() != null ? request.getIssuingDate() : lab.getIssuingDate();
        LocalDate expiry = request.getExpiryDate() != null ? request.getExpiryDate() : lab.getExpiryDate();
        if (issuing != null && expiry != null && expiry.isBefore(issuing)) {
            throw new IllegalStateException("Expiry date cannot be before issuing date");
        }

        request.setPreviousFileUrl(type.getUrl(lab));
        type.setUrl(lab, request.getNewFileUrl());
        if (request.getIssuingDate() != null) {
            lab.setIssuingDate(request.getIssuingDate());
        }
        if (request.getExpiryDate() != null) {
            lab.setExpiryDate(request.getExpiryDate());
        }
        labRepository.save(lab);

        request.setStatus(LabApprovalStatus.APPROVED);
        request.setRejectionReason(null);
        request.setReviewedAt(LocalDateTime.now());
        LabCertificateRequest saved = requestRepository.save(request);
        log.info("Certificate request {} ({}) approved for lab {}", saved.getId(), type.getParam(), lab.getId());
        return toDto(saved, lab.getName(), usernameOf(saved.getRequestedByUserId()));
    }

    /**
     * Rejects the request. The lab and every file are left as they are.
     */
    @Transactional
    public LabCertificateRequestDTO reject(Long requestId, String reason) {
        LabCertificateRequest request = getPendingForUpdate(requestId);
        request.setStatus(LabApprovalStatus.REJECTED);
        request.setRejectionReason(reason.trim());
        request.setReviewedAt(LocalDateTime.now());
        LabCertificateRequest saved = requestRepository.save(request);
        log.info("Certificate request {} rejected", saved.getId());
        String labName = labRepository.findById(saved.getLabId()).map(Lab::getName).orElse(null);
        return toDto(saved, labName, usernameOf(saved.getRequestedByUserId()));
    }

    // ------------------------------------------------------------------ helpers

    private LabCertificateRequest getPendingForUpdate(Long requestId) {
        LabCertificateRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new NoSuchElementException("Certificate request not found"));
        if (request.getStatus() != LabApprovalStatus.PENDING) {
            throw new IllegalStateException("Certificate request is not pending (current status: "
                    + request.getStatus() + ")");
        }
        return request;
    }

    private Lab getAccessibleLab(User currentUser, Long labId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new NoSuchElementException("Lab not found"));
        if (!userRepository.existsByIdAndLabsId(currentUser.getId(), labId)) {
            throw new IllegalStateException("User is not a member of this lab");
        }
        return lab;
    }

    private void validateDates(LocalDate issuingDate, LocalDate expiryDate) {
        if (issuingDate != null && expiryDate != null && expiryDate.isBefore(issuingDate)) {
            throw new IllegalArgumentException("Expiry date cannot be before issuing date");
        }
    }

    /**
     * The upload happens before the request row is saved. If this transaction does not commit, the file would be
     * an orphan, so it is removed. This only ever touches the file uploaded by this very call.
     */
    private void deleteUploadIfNotCommitted(String url) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    log.info("Deleting S3 object of a certificate request that was not saved: {}", url);
                    s3StorageService.deleteObjectByUrl(url);
                }
            }
        });
    }

    private List<LabCertificateRequestDTO> toDtos(List<LabCertificateRequest> requests, Map<Long, String> labNames) {
        Set<Long> userIds = requests.stream()
                .map(LabCertificateRequest::getRequestedByUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> usernames = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> usernames.put(user.getId(), user.getUsername()));
        return requests.stream()
                .map(r -> toDto(r, labNames.get(r.getLabId()), usernames.get(r.getRequestedByUserId())))
                .toList();
    }

    private String usernameOf(Long userId) {
        return userId == null ? null : userRepository.findById(userId).map(User::getUsername).orElse(null);
    }

    private LabCertificateRequestDTO toDto(LabCertificateRequest request, String labName, String requestedBy) {
        return LabCertificateRequestDTO.builder()
                .id(request.getId())
                .labId(request.getLabId())
                .labName(labName)
                .documentType(LabDocumentType.valueOf(request.getDocumentType()).getParam())
                .requestedBy(requestedBy)
                .newFileUrl(request.getNewFileUrl())
                .previousFileUrl(request.getPreviousFileUrl())
                .issuingDate(request.getIssuingDate())
                .expiryDate(request.getExpiryDate())
                .status(request.getStatus().name())
                .rejectionReason(request.getRejectionReason())
                .requestedAt(request.getRequestedAt())
                .reviewedAt(request.getReviewedAt())
                .build();
    }
}
