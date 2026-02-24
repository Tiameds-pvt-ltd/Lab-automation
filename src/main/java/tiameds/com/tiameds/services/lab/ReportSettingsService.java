package tiameds.com.tiameds.services.lab;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.dto.lab.*;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.ReportRoleSetting;
import tiameds.com.tiameds.entity.ReportSettings;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.ReportSettingsRepository;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.services.S3StorageService;
import tiameds.com.tiameds.entity.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ReportSettingsService {

    private final ReportSettingsRepository reportSettingsRepository;
    private final LabRepository labRepository;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    private static final String DEFAULT_DISCLAIMER = "This laboratory report is intended for clinical correlation only. Results should be interpreted by a qualified medical professional.";

    public ReportSettingsService(ReportSettingsRepository reportSettingsRepository,
                                LabRepository labRepository,
                                UserRepository userRepository,
                                S3StorageService s3StorageService) {
        this.reportSettingsRepository = reportSettingsRepository;
        this.labRepository = labRepository;
        this.userRepository = userRepository;
        this.s3StorageService = s3StorageService;
    }

    @Transactional(readOnly = true)
    public Optional<ReportSettingsResponseDTO> getByLabId(Long labId) {
        return reportSettingsRepository.findByLabIdWithRoles(labId)
                .map(this::toResponseDTO);
    }

    @Transactional
    public ReportSettingsResponseDTO create(Long labId, ReportSettingsPayloadDTO payload) {
        if (reportSettingsRepository.existsByLab_Id(labId)) {
            throw new IllegalStateException("Report settings already exist for this lab. Use PUT to update.");
        }
        Lab lab = labRepository.findById(labId).orElseThrow(() -> new IllegalArgumentException("Lab not found"));
        ReportSettings entity = mapPayloadToEntity(payload, lab);
        entity = reportSettingsRepository.save(entity);
        return toResponseDTO(entity);
    }

    @Transactional
    public ReportSettingsResponseDTO update(Long labId, ReportSettingsPayloadDTO payload) {
        ReportSettings entity = reportSettingsRepository.findByLabIdWithRoles(labId)
                .orElseThrow(() -> new IllegalArgumentException("Report settings not found for this lab. Use POST to create."));
        Lab lab = entity.getLab();
        if (lab == null) {
            lab = labRepository.findById(labId).orElseThrow(() -> new IllegalArgumentException("Lab not found"));
        }
        // Capture old signature URLs before replacing roles (for S3 cleanup - done in task 5)
        List<String> oldSignatureUrls = entity.getRoles().stream()
                .map(ReportRoleSetting::getSignatureUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();
        // Update main fields
        updateEntityFromPayload(entity, payload);
        // Replace roles: clear and re-add
        entity.getRoles().clear();
        List<ReportRolePayloadDTO> roles = payload.getRoles() != null ? payload.getRoles() : Collections.emptyList();
        for (int i = 0; i < roles.size(); i++) {
            ReportRolePayloadDTO r = roles.get(i);
            ReportRoleSetting roleEntity = new ReportRoleSetting();
            roleEntity.setReportSettings(entity);
            roleEntity.setRole(r.getRole() != null ? r.getRole() : "");
            roleEntity.setDisplayName(r.getDisplayName() != null ? r.getDisplayName() : "");
            roleEntity.setDesignation(r.getDesignation() != null ? r.getDesignation() : "");
            roleEntity.setSignatureUrl(r.getSignatureUrl() != null ? r.getSignatureUrl() : "");
            roleEntity.setEnabled(r.getEnabled() != null ? r.getEnabled() : true);
            roleEntity.setSortOrder(r.getSortOrder() != null ? r.getSortOrder() : i);
            entity.getRoles().add(roleEntity);
        }
        entity = reportSettingsRepository.save(entity);
        // S3 cleanup: delete old signature URLs that are no longer in use (task 5 will add this)
        deleteReplacedSignatureUrls(oldSignatureUrls, payload.getRoles());
        return toResponseDTO(entity);
    }

    /**
     * Best-effort S3 cleanup: delete old signature URLs that were replaced.
     * Called from update() after DB save succeeds.
     */
    private void deleteReplacedSignatureUrls(List<String> oldUrls, List<ReportRolePayloadDTO> newRoles) {
        if (oldUrls == null || oldUrls.isEmpty()) return;
        List<String> newUrls = new ArrayList<>();
        if (newRoles != null) {
            for (ReportRolePayloadDTO r : newRoles) {
                if (r.getSignatureUrl() != null && !r.getSignatureUrl().isBlank()) {
                    newUrls.add(r.getSignatureUrl());
                }
            }
        }
        for (String oldUrl : oldUrls) {
            if (!newUrls.contains(oldUrl)) {
                s3StorageService.deleteObjectByUrl(oldUrl);
            }
        }
    }

    /**
     * Generates a presigned S3 URL for uploading a signature image.
     * S3 key pattern: labs/{labId}/signatures/{uuid}-{fileName}
     */
    @Transactional(readOnly = true)
    public ReportSignatureUploadResponseDTO createSignatureUploadUrl(User currentUser, Long labId, ReportSignatureUploadRequestDTO request) {
        if (request == null || request.getFileName() == null || request.getFileName().isBlank()) {
            throw new IllegalArgumentException("fileName is required");
        }
        if (request.getFileType() == null || request.getFileType().isBlank()) {
            throw new IllegalArgumentException("fileType is required");
        }
        if (!userRepository.existsByIdAndLabsId(currentUser.getId(), labId)) {
            throw new IllegalStateException("User is not a member of this lab");
        }
        String key = s3StorageService.generateUniqueKey("labs/" + labId + "/signatures/", request.getFileName());
        String[] urls = s3StorageService.createPresignedPutUrl(key, request.getFileType());
        return new ReportSignatureUploadResponseDTO(urls[0], urls[1]);
    }

    private ReportSettings mapPayloadToEntity(ReportSettingsPayloadDTO payload, Lab lab) {
        ReportSettings entity = new ReportSettings();
        entity.setLab(lab);
        updateEntityFromPayload(entity, payload);
        List<ReportRolePayloadDTO> roles = payload.getRoles() != null ? payload.getRoles() : Collections.emptyList();
        for (int i = 0; i < roles.size(); i++) {
            ReportRolePayloadDTO r = roles.get(i);
            ReportRoleSetting roleEntity = new ReportRoleSetting();
            roleEntity.setReportSettings(entity);
            roleEntity.setRole(r.getRole() != null ? r.getRole() : "");
            roleEntity.setDisplayName(r.getDisplayName() != null ? r.getDisplayName() : "");
            roleEntity.setDesignation(r.getDesignation() != null ? r.getDesignation() : "");
            roleEntity.setSignatureUrl(r.getSignatureUrl() != null ? r.getSignatureUrl() : "");
            roleEntity.setEnabled(r.getEnabled() != null ? r.getEnabled() : true);
            roleEntity.setSortOrder(r.getSortOrder() != null ? r.getSortOrder() : i);
            entity.getRoles().add(roleEntity);
        }
        return entity;
    }

    private void updateEntityFromPayload(ReportSettings entity, ReportSettingsPayloadDTO payload) {
        entity.setTemplateId(payload.getTemplateId() != null ? payload.getTemplateId() : "templateA");
        entity.setHeaderEnabled(payload.getHeaderEnabled() != null ? payload.getHeaderEnabled() : true);
        entity.setHeaderRequired(payload.getHeaderRequired() != null ? payload.getHeaderRequired() : false);
        entity.setFontSize(payload.getFontSize() != null ? payload.getFontSize() : 12);
        entity.setTextSize(payload.getTextSize() != null ? payload.getTextSize() : "Medium");
        entity.setTextColor(payload.getTextColor() != null ? payload.getTextColor() : "#111827");
        entity.setSignaturePlacement(payload.getSignaturePlacement() != null ? payload.getSignaturePlacement() : "bottom-right");
        entity.setSignatureColumns(payload.getSignatureColumns() != null ? payload.getSignatureColumns() : 2);
        entity.setDisclaimerEnabled(payload.getDisclaimerEnabled() != null ? payload.getDisclaimerEnabled() : true);
        entity.setDisclaimerText(payload.getDisclaimerText() != null && !payload.getDisclaimerText().isBlank()
                ? payload.getDisclaimerText() : DEFAULT_DISCLAIMER);
    }

    private ReportSettingsResponseDTO toResponseDTO(ReportSettings entity) {
        List<ReportRoleResponseDTO> roles = entity.getRoles().stream()
                .map(r -> new ReportRoleResponseDTO(
                        r.getId(),
                        r.getRole(),
                        r.getDisplayName(),
                        r.getDesignation(),
                        r.getSignatureUrl(),
                        r.getEnabled(),
                        r.getSortOrder()
                ))
                .toList();
        return new ReportSettingsResponseDTO(
                entity.getId(),
                entity.getLabId(),
                entity.getTemplateId(),
                entity.getHeaderEnabled(),
                entity.getHeaderRequired(),
                entity.getFontSize(),
                entity.getTextSize(),
                entity.getTextColor(),
                entity.getSignaturePlacement(),
                entity.getSignatureColumns(),
                entity.getDisclaimerEnabled(),
                entity.getDisclaimerText(),
                roles,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
