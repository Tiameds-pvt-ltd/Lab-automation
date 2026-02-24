package tiameds.com.tiameds.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

/**
 * Shared S3 operations for presigned uploads and object deletion.
 * Used by LabService (lab logo) and ReportSettingsService (signature images).
 */
@Service
public class S3StorageService {

    private final String s3Bucket;
    private final String s3Region;
    private final String s3CdnBaseUrl;
    private final long presignExpiryMinutes;

    public S3StorageService(
            @Value("${aws.s3.bucket:}") String s3Bucket,
            @Value("${aws.s3.region:}") String s3Region,
            @Value("${aws.s3.cdn-base-url:}") String s3CdnBaseUrl,
            @Value("${aws.s3.presign-expiry-minutes:10}") long presignExpiryMinutes
    ) {
        this.s3Bucket = s3Bucket;
        this.s3Region = s3Region;
        this.s3CdnBaseUrl = s3CdnBaseUrl;
        this.presignExpiryMinutes = presignExpiryMinutes;
    }

    /**
     * Generates a presigned PUT URL for uploading a file to S3.
     *
     * @param key         S3 object key (e.g. labs/5/signatures/uuid-file.png)
     * @param contentType Optional content type for the object
     * @return Array of [uploadUrl, fileUrl]
     */
    public String[] createPresignedPutUrl(String key, String contentType) {
        if (s3Bucket == null || s3Bucket.isBlank() || s3Region == null || s3Region.isBlank()) {
            throw new IllegalStateException("S3 bucket and region must be configured");
        }
        PutObjectRequest.Builder putBuilder = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(key);
        if (contentType != null && !contentType.isBlank()) {
            putBuilder.contentType(contentType);
        }
        PutObjectRequest putObjectRequest = putBuilder.build();
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
            return new String[]{uploadUrl, fileUrl};
        }
    }

    public String buildFileUrl(String key) {
        if (s3CdnBaseUrl != null && !s3CdnBaseUrl.isBlank()) {
            String base = s3CdnBaseUrl.trim();
            if (base.startsWith("http://") || base.startsWith("https://")) {
                String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
                return normalizedBase + "/" + key;
            }
        }
        return "https://" + s3Bucket + ".s3." + s3Region + ".amazonaws.com/" + key;
    }

    public String sanitizeFileName(String fileName) {
        String normalized = fileName.replace("\\", "/");
        String baseName = normalized.substring(normalized.lastIndexOf("/") + 1);
        return baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Best-effort deletion of an S3 object by its public URL.
     * Swallows exceptions so callers are not affected.
     */
    public void deleteObjectByUrl(String fileUrl) {
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
            // Best-effort: do not fail the caller
        }
    }

    private String extractS3Key(String fileUrl) {
        URI uri = URI.create(fileUrl);
        String host = uri.getHost();
        if (host == null) return null;
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (host.startsWith(s3Bucket + ".s3.")) {
            return path.startsWith("/") ? path.substring(1) : path;
        }
        if (host.startsWith("s3.") || host.startsWith("s3-") || host.equals("s3.amazonaws.com")) {
            String trimmed = path.startsWith("/") ? path.substring(1) : path;
            if (!trimmed.startsWith(s3Bucket + "/")) return null;
            return trimmed.substring((s3Bucket + "/").length());
        }
        if (fileUrl.contains(".amazonaws.com/")) {
            String[] parts = fileUrl.split("\\.amazonaws\\.com/", 2);
            if (parts.length == 2) return parts[1];
        }
        return null;
    }

    public String generateUniqueKey(String prefix, String fileName) {
        return prefix + UUID.randomUUID() + "-" + sanitizeFileName(fileName);
    }
}
