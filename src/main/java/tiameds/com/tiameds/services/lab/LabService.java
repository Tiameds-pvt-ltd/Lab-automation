package tiameds.com.tiameds.services.lab;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import tiameds.com.tiameds.dto.lab.LabListFullDTO;
import tiameds.com.tiameds.dto.lab.LabLogoUploadRequestDTO;
import tiameds.com.tiameds.dto.lab.LabLogoUploadResponseDTO;
import tiameds.com.tiameds.dto.lab.LabMemberDTO;
import tiameds.com.tiameds.dto.lab.LabRequestDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.utils.LabAccessableFilter;

import java.time.Duration;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class LabService {
    private final LabRepository labRepository;
    private final LabAccessableFilter labAccessableFilter;
    private final UserRepository userRepository;
    private final String s3Bucket;
    private final String s3Region;
    private final String s3CdnBaseUrl;
    private final long presignExpiryMinutes;

    public LabService(
            LabRepository labRepository,
            LabAccessableFilter labAccessableFilter,
            UserRepository userRepository,
            @Value("${aws.s3.bucket:}") String s3Bucket,
            @Value("${aws.s3.region:}") String s3Region,
            @Value("${aws.s3.cdn-base-url:}") String s3CdnBaseUrl,
            @Value("${aws.s3.presign-expiry-minutes:10}") long presignExpiryMinutes
    ) {
        this.labRepository = labRepository;
        this.labAccessableFilter = labAccessableFilter;
        this.userRepository = userRepository;
        this.s3Bucket = s3Bucket;
        this.s3Region = s3Region;
        this.s3CdnBaseUrl = s3CdnBaseUrl;
        this.presignExpiryMinutes = presignExpiryMinutes;
    }

    @Transactional(readOnly = true)
    public List<LabListFullDTO> getLabsCreatedByUser(User currentUser) {
        List<Lab> labs = labRepository.findByCreatedBy(currentUser);
        return labs.stream()
                .map(this::toLabListFullDTO)
                .toList();
    }

    public LabListFullDTO toLabListFullDTO(Lab lab) {
        return new LabListFullDTO(
                lab.getId(),
                lab.getName(),
                lab.getAddress(),
                lab.getCity(),
                lab.getState(),
                lab.getIsActive(),
                lab.getDescription(),
                lab.getLabLogo(),
                lab.getLicenseNumber(),
                lab.getLabType(),
                lab.getLabZip(),
                lab.getLabCountry(),
                lab.getLabPhone(),
                lab.getLabEmail(),
                lab.getDirectorName(),
                lab.getDirectorEmail(),
                lab.getDirectorPhone(),
                lab.getCertificationBody(),
                lab.getLabCertificate(),
                lab.getDirectorGovtId(),
                lab.getLabBusinessRegistration(),
                lab.getLabLicense(),
                lab.getTaxId(),
                lab.getLabAccreditation(),
                lab.getDataPrivacyAgreement(),
                lab.getCreatedAt(),
                lab.getUpdatedAt(),
                lab.getCreatedByName()
        );
    }

    @Transactional(readOnly = true)
    public List<LabMemberDTO> getAllMembersOfALab(Long labId) {
        Lab lab = labRepository.findLabWithMembers(labId).orElse(null);
        if (lab == null) {
            return Collections.emptyList();
        }
        Set<User> members = lab.getMembers();   
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        return members.stream()
                .map(member -> new LabMemberDTO(
                        member.getId(),
                        member.getUsername(),
                        member.getFirstName(),
                        member.getLastName(),
                        member.getEmail(),
                        member.getPhone(),
                        member.getCity(),
                        member.getState(),
                        member.isEnabled(),
                        member.getUserCode()
                ))
                .toList();
    }

    @Transactional
    public UpdateLabResult updateLabById(User currentUser, Long labId, LabRequestDTO labRequestDTO) {
        Lab lab = labRepository.findById(labId).orElse(null);
        if (lab == null) {
            return new UpdateLabResult(HttpStatus.NOT_FOUND, "Lab not found", null);
        }
        // if (!labAccessableFilter.isLabAccessible(labId)) {
        //     return new UpdateLabResult(HttpStatus.UNAUTHORIZED, "Lab not accessible", null);
        // }
        if (!userRepository.existsByIdAndLabsId(currentUser.getId(), labId)) {
            return new UpdateLabResult(HttpStatus.UNAUTHORIZED, "User is not a member of this lab", null);
        }
        // Capture old logo URL so we can delete it after a successful update.
        String oldLogoUrl = lab.getLabLogo();
        lab.setName(labRequestDTO.getName());
        lab.setAddress(labRequestDTO.getAddress());
        lab.setCity(labRequestDTO.getCity());
        lab.setState(labRequestDTO.getState());
        lab.setIsActive(labRequestDTO.getIsActive());
        lab.setDescription(labRequestDTO.getDescription());
        String newLogoUrl = labRequestDTO.getLabLogo();
        if (newLogoUrl != null && !newLogoUrl.isBlank()) {
            lab.setLabLogo(newLogoUrl);
        }
        lab.setLicenseNumber(labRequestDTO.getLicenseNumber());
        lab.setLabType(labRequestDTO.getLabType());
        lab.setLabZip(labRequestDTO.getLabZip());
        lab.setLabCountry(labRequestDTO.getLabCountry());
        lab.setLabPhone(labRequestDTO.getLabPhone());
        lab.setLabEmail(labRequestDTO.getLabEmail());
        lab.setDirectorName(labRequestDTO.getDirectorName());
        lab.setDirectorEmail(labRequestDTO.getDirectorEmail());
        lab.setDirectorPhone(labRequestDTO.getDirectorPhone());
        lab.setCertificationBody(labRequestDTO.getCertificationBody());
        lab.setLabCertificate(labRequestDTO.getLabCertificate());
        lab.setDirectorGovtId(labRequestDTO.getDirectorGovtId());
        lab.setLabBusinessRegistration(labRequestDTO.getLabBusinessRegistration());
        lab.setLabLicense(labRequestDTO.getLabLicense());
        lab.setTaxId(labRequestDTO.getTaxId());
        lab.setLabAccreditation(labRequestDTO.getLabAccreditation());
        lab.setDataPrivacyAgreement(labRequestDTO.getDataPrivacyAgreement());
        labRepository.save(lab);
        // Best-effort cleanup: delete previous logo only after DB update succeeds.
        if (newLogoUrl != null && !newLogoUrl.isBlank()
                && oldLogoUrl != null && !oldLogoUrl.isBlank()
                && !oldLogoUrl.equals(newLogoUrl)) {
            deleteS3ObjectByUrl(oldLogoUrl);
        }
        return new UpdateLabResult(HttpStatus.OK, "Lab updated successfully", toLabListFullDTO(lab));
    }

    @Transactional(readOnly = true)
    public LabLogoUploadResponseDTO createLabLogoUploadUrl(User currentUser, LabLogoUploadRequestDTO request) {
        if (request == null || request.getLabId() == null) {
            throw new IllegalArgumentException("labId is required");
        }
        if (request.getFileName() == null || request.getFileName().isBlank()) {
            throw new IllegalArgumentException("fileName is required");
        }
        if (request.getFileType() == null || request.getFileType().isBlank()) {
            throw new IllegalArgumentException("fileType is required");
        }
        if (s3Bucket == null || s3Bucket.isBlank() || s3Region == null || s3Region.isBlank()) {
            throw new IllegalStateException("S3 bucket and region must be configured");
        }
        Long labId = request.getLabId();
//        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
//        if (!isAccessible) {
//            throw new IllegalStateException("Lab not accessible");
//        }
        if (!userRepository.existsByIdAndLabsId(currentUser.getId(), labId)) {
            throw new IllegalStateException("User is not a member of this lab");
        }

        String safeFileName = sanitizeFileName(request.getFileName());
        String key = "labs/" + labId + "/" + UUID.randomUUID() + "-" + safeFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(key)
//                .contentType(request.getFileType())
                .build();

        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(s3Region))
                .build()) {
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(presignExpiryMinutes))
                    .putObjectRequest(putObjectRequest)
                    .build();
            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
            String uploadUrl = presignedRequest.url().toString();
            String fileUrl = buildFileUrl(key);
            return new LabLogoUploadResponseDTO(uploadUrl, fileUrl);
        }
    }

    private String buildFileUrl(String key) {
        if (s3CdnBaseUrl != null && !s3CdnBaseUrl.isBlank()) {
            String base = s3CdnBaseUrl.trim();
            if (base.startsWith("http://") || base.startsWith("https://")) {
                String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
                return normalizedBase + "/" + key;
            }
        }
        return "https://" + s3Bucket + ".s3." + s3Region + ".amazonaws.com/" + key;
    }

    private String sanitizeFileName(String fileName) {
        String normalized = fileName.replace("\\", "/");
        String baseName = normalized.substring(normalized.lastIndexOf("/") + 1);
        return baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Deletes an S3 object from an absolute S3 URL.
     * This is best-effort cleanup so the update flow never fails if delete fails.
     */
    private void deleteS3ObjectByUrl(String fileUrl) {
        try {
            String key = extractS3Key(fileUrl);
            if (key == null || key.isBlank()) {
                return;
            }
            try (S3Client s3Client = S3Client.builder()
                    .region(Region.of(s3Region))
                    .build()) {
                DeleteObjectRequest request = DeleteObjectRequest.builder()
                        .bucket(s3Bucket)
                        .key(key)
                        .build();
                s3Client.deleteObject(request);
            }
        } catch (Exception ignored) {
            // Intentionally swallow exceptions to avoid breaking the update flow.
        }
    }

    /**
     * Extracts the S3 object key from standard S3 URLs.
     * Supports:
     * 1) https://<bucket>.s3.<region>.amazonaws.com/<key>
     * 2) https://s3.<region>.amazonaws.com/<bucket>/<key>
     */
    private String extractS3Key(String fileUrl) {
        URI uri = URI.create(fileUrl);
        String host = uri.getHost();
        if (host == null) {
            return null;
        }
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (host.startsWith(s3Bucket + ".s3.")) {
            return path.startsWith("/") ? path.substring(1) : path;
        }
        if (host.startsWith("s3.") || host.startsWith("s3-") || host.equals("s3.amazonaws.com")) {
            String trimmed = path.startsWith("/") ? path.substring(1) : path;
            if (!trimmed.startsWith(s3Bucket + "/")) {
                return null;
            }
            return trimmed.substring((s3Bucket + "/").length());
        }
        if (fileUrl.contains(".amazonaws.com/")) {
            String[] parts = fileUrl.split("\\.amazonaws\\.com/", 2);
            if (parts.length == 2) {
                return parts[1];
            }
        }
        return null;
    }

    public static class UpdateLabResult {
        private final HttpStatus status;
        private final String message;
        private final LabListFullDTO data;

        public UpdateLabResult(HttpStatus status, String message, LabListFullDTO data) {
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public HttpStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public LabListFullDTO getData() {
            return data;
        }
    }
}
