package tiameds.com.tiameds.services.lab;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.services.S3StorageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Add / update / delete of the lab logo and lab certificates stored in S3 (URL kept on the labs table).
 *
 * Each operation is all-or-nothing:
 * - Every file is validated before anything is uploaded.
 * - If any upload or the DB save fails, the DB transaction is rolled back and every object
 *   uploaded by this call is deleted from S3.
 * - Objects that are replaced or deleted are only removed from S3 after the DB commit succeeds.
 */
@Slf4j
@Service
public class LabDocumentService {

    private static final long MAX_FILE_SIZE_BYTES = 1024 * 1024; // 1 MB

    private final LabRepository labRepository;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    public LabDocumentService(LabRepository labRepository,
                              UserRepository userRepository,
                              S3StorageService s3StorageService) {
        this.labRepository = labRepository;
        this.userRepository = userRepository;
        this.s3StorageService = s3StorageService;
    }

    /**
     * Returns the stored URL (or null) for every document type.
     */
    @Transactional(readOnly = true)
    public Map<String, String> getDocuments(User currentUser, Long labId) {
        Lab lab = getAccessibleLab(currentUser, labId);
        Map<String, String> result = new LinkedHashMap<>();
        for (LabDocumentType type : LabDocumentType.values()) {
            result.put(type.getParam(), type.getUrl(lab));
        }
        return result;
    }

    /**
     * Adds files for document types that do not have a file yet.
     */
    @Transactional
    public Map<String, String> addDocuments(User currentUser, Long labId, Map<LabDocumentType, MultipartFile> files) {
        Lab lab = getAccessibleLab(currentUser, labId);
        requireFiles(files);
        for (LabDocumentType type : files.keySet()) {
            if (hasUrl(type.getUrl(lab))) {
                throw new IllegalArgumentException(type.getParam() + " already exists. Use update to replace it");
            }
        }
        return storeFiles(lab, files);
    }

    /**
     * Replaces files for document types that already have a file. The old S3 objects are
     * deleted after the DB commit succeeds.
     */
    @Transactional
    public Map<String, String> updateDocuments(User currentUser, Long labId, Map<LabDocumentType, MultipartFile> files) {
        Lab lab = getAccessibleLab(currentUser, labId);
        requireFiles(files);
        for (LabDocumentType type : files.keySet()) {
            if (!hasUrl(type.getUrl(lab))) {
                throw new IllegalArgumentException(type.getParam() + " does not exist. Use add to upload it");
            }
        }
        return storeFiles(lab, files);
    }

    /**
     * Clears the URL for each requested document type; the S3 objects are deleted after the DB commit succeeds.
     */
    @Transactional
    public Map<String, String> deleteDocuments(User currentUser, Long labId, Set<LabDocumentType> types) {
        Lab lab = getAccessibleLab(currentUser, labId);
        if (types == null || types.isEmpty()) {
            throw new IllegalArgumentException("At least one document type is required");
        }
        for (LabDocumentType type : types) {
            if (!hasUrl(type.getUrl(lab))) {
                throw new IllegalArgumentException(type.getParam() + " does not exist");
            }
        }

        List<String> obsoleteUrls = new ArrayList<>();
        Map<String, String> result = new LinkedHashMap<>();
        for (LabDocumentType type : types) {
            obsoleteUrls.add(type.getUrl(lab));
            type.setUrl(lab, null);
            result.put(type.getParam(), null);
        }
        labRepository.saveAndFlush(lab);

        registerS3Cleanup(new ArrayList<>(), obsoleteUrls);
        return result;
    }

    private Map<String, String> storeFiles(Lab lab, Map<LabDocumentType, MultipartFile> files) {
        // Validate everything first so nothing is uploaded for a request that will be rejected
        Map<LabDocumentType, ValidatedFile> validated = new LinkedHashMap<>();
        files.forEach((type, file) -> validated.put(type, validate(type, file)));

        List<String> uploadedUrls = new ArrayList<>();
        List<String> obsoleteUrls = new ArrayList<>();
        // Registered before uploading: any exception below rolls back the transaction,
        // which triggers deletion of everything already uploaded.
        registerS3Cleanup(uploadedUrls, obsoleteUrls);

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<LabDocumentType, ValidatedFile> entry : validated.entrySet()) {
            LabDocumentType type = entry.getKey();
            ValidatedFile file = entry.getValue();
            String key = s3StorageService.generateUniqueKey(
                    "labs/" + lab.getId() + "/documents/" + type.getFolder() + "/", file.fileName());
            String url = s3StorageService.uploadObject(key, file.content(), file.contentType());
            uploadedUrls.add(url);

            String oldUrl = type.getUrl(lab);
            if (hasUrl(oldUrl)) {
                obsoleteUrls.add(oldUrl);
            }
            type.setUrl(lab, url);
            result.put(type.getParam(), url);
        }
        labRepository.saveAndFlush(lab);
        return result;
    }

    /**
     * Rolled back / failed: delete the objects uploaded by this call.
     * Committed: delete the objects that were replaced or removed.
     */
    private void registerS3Cleanup(List<String> uploadedUrls, List<String> obsoleteUrls) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                List<String> toDelete = status == STATUS_COMMITTED ? obsoleteUrls : uploadedUrls;
                for (String url : toDelete) {
                    log.info("Deleting S3 object after lab document {}: {}",
                            status == STATUS_COMMITTED ? "commit" : "rollback", url);
                    s3StorageService.deleteObjectByUrl(url);
                }
            }
        });
    }

    private Lab getAccessibleLab(User currentUser, Long labId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new NoSuchElementException("Lab not found"));
        if (!userRepository.existsByIdAndLabsId(currentUser.getId(), labId)) {
            throw new IllegalStateException("User is not a member of this lab");
        }
        return lab;
    }

    private void requireFiles(Map<LabDocumentType, MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required");
        }
    }

    private boolean hasUrl(String url) {
        return url != null && !url.isBlank();
    }

    private ValidatedFile validate(LabDocumentType type, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(type.getParam() + " file is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(type.getParam() + " must be smaller than 1 MB");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + type.getParam() + " file", e);
        }

        // Content type is detected from the file signature, not trusted from the client
        String contentType = detectContentType(content);
        boolean isImage = "image/png".equals(contentType) || "image/jpeg".equals(contentType);
        boolean allowed = type == LabDocumentType.LAB_LOGO ? isImage : (isImage || "application/pdf".equals(contentType));
        if (!allowed) {
            throw new IllegalArgumentException(type.getParam() + " must be "
                    + (type == LabDocumentType.LAB_LOGO ? "a PNG or JPEG image" : "a PDF, PNG or JPEG file"));
        }

        String fileName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? type.getFolder()
                : file.getOriginalFilename();
        return new ValidatedFile(fileName, content, contentType);
    }

    private String detectContentType(byte[] content) {
        if (startsWith(content, 0x25, 0x50, 0x44, 0x46)) {
            return "application/pdf";
        }
        if (startsWith(content, 0x89, 0x50, 0x4E, 0x47)) {
            return "image/png";
        }
        if (startsWith(content, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        return null;
    }

    private boolean startsWith(byte[] content, int... signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((content[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private record ValidatedFile(String fileName, byte[] content, String contentType) {
    }
}
