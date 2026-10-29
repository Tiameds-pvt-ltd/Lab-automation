package tiameds.com.tiameds.controller.onboarding;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.services.lab.LabDocumentService;
import tiameds.com.tiameds.services.lab.LabDocumentType;
import tiameds.com.tiameds.utils.ApiResponseHelper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Document upload for the onboarding form. The lab does not exist yet, so the files are stored under a random
 * folder and the returned URLs are sent in the /public/onboarding/complete request body.
 * The endpoint is public, so it is rate limited per client IP.
 *
 * Part names (files): labLogo, organizationGstinCertificate, gstinCertificate (lab GST), establishmentRegistrationCertificate,
 * biomedicalWasteAuthorizationCertificate, nablAccreditationCertificate.
 */
@Slf4j
@RestController
@RequestMapping("/public/onboarding/documents")
@Tag(name = "Onboarding - Documents", description = "Upload lab logo and certificates before completing onboarding")
public class OnboardingDocumentController {

    private static final int MAX_REQUESTS_PER_HOUR = 10;
    private static final int MAX_TRACKED_IPS = 10_000;

    // In-memory per-IP limit (same approach as the login rate limit)
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    private final LabDocumentService labDocumentService;

    public OnboardingDocumentController(LabDocumentService labDocumentService) {
        this.labDocumentService = labDocumentService;
    }

    @Operation(summary = "Upload onboarding documents",
            description = "Uploads the files to S3 and returns their URLs. Send these URLs in the matching fields of "
                    + "/public/onboarding/complete. Limited to " + MAX_REQUESTS_PER_HOUR + " requests per hour per IP.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocuments(@RequestParam Map<String, MultipartFile> files,
                                             HttpServletRequest request) {
        if (ipBuckets.size() > MAX_TRACKED_IPS) {
            ipBuckets.clear(); // keeps memory bounded; limits restart for everyone, which is acceptable
        }
        if (!ipBuckets.computeIfAbsent(getClientIp(request), ip -> newBucket()).tryConsume(1)) {
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Too many upload requests. Please try again later.", HttpStatus.TOO_MANY_REQUESTS, null);
        }

        try {
            Map<LabDocumentType, MultipartFile> typed = new LinkedHashMap<>();
            files.forEach((name, file) -> typed.put(LabDocumentType.fromParam(name), file));

            Map<String, String> urls = labDocumentService.uploadFiles(
                    LabDocumentService.ONBOARDING_S3_PREFIX + UUID.randomUUID() + "/documents/", typed);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Documents uploaded successfully", HttpStatus.OK, urls);
        } catch (IllegalArgumentException e) {
            return ApiResponseHelper.successResponseWithDataAndMessage(e.getMessage(), HttpStatus.BAD_REQUEST, null);
        } catch (Exception e) {
            log.error("Onboarding document upload failed: {}", e.getMessage(), e);
            return ApiResponseHelper.successResponseWithDataAndMessage(
                    "Document upload failed. No files were saved.", HttpStatus.INTERNAL_SERVER_ERROR, null);
        }
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(MAX_REQUESTS_PER_HOUR,
                        Refill.intervally(MAX_REQUESTS_PER_HOUR, Duration.ofHours(1))))
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
