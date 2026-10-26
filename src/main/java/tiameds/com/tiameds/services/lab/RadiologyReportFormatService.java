package tiameds.com.tiameds.services.lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tiameds.com.tiameds.entity.RadiologyReportFormat;
import tiameds.com.tiameds.repository.RadiologyReportFormatRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Service
public class RadiologyReportFormatService {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RadiologyReportFormatRepository repository;

    public RadiologyReportFormatService(RadiologyReportFormatRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> upload(Long labId, MultipartFile file, String documentName,
                                      Long doctorId, String doctorName, String username) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Report format file is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("The document is larger than 5 MB");
        }
        String name = documentName != null && !documentName.isBlank() ? documentName : file.getOriginalFilename();
        String lower = name == null ? "" : name.toLowerCase();
        if (!lower.endsWith(".doc") && !lower.endsWith(".docx")) {
            throw new IllegalArgumentException("Please upload a Word document (.doc or .docx)");
        }

        RadiologyReportFormat format = new RadiologyReportFormat();
        format.setLabId(labId);
        format.setDocumentName(name.trim());
        format.setContentType(file.getContentType());
        format.setFileSize(file.getSize());
        byte[] data = file.getBytes();
        format.setFileData(data);
        format.setContentJson(toJson(data, name));
        format.setDoctorId(doctorId);
        format.setDoctorName(doctorName == null || doctorName.isBlank() ? null : doctorName.trim());
        format.setUploadedBy(username);
        RadiologyReportFormat saved = repository.save(format);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("documentName", saved.getDocumentName());
        result.put("doctorId", saved.getDoctorId());
        result.put("doctorName", saved.getDoctorName());
        result.put("uploadedAt", saved.getCreatedAt());
        result.put("contentParsed", saved.getContentJson() != null);
        return result;
    }

    // The original file is always kept; a file that cannot be read (corrupt, or not really Word) still uploads, without JSON.
    private String toJson(byte[] data, String name) {
        try {
            return WordDocumentJsonConverter.toJson(data);
        } catch (Exception e) {
            log.warn("Could not convert report format '{}' to JSON: {}", name, e.getMessage());
            return null;
        }
    }

    // Metadata plus the parsed text/tables; content is null for formats uploaded before JSON was stored.
    public Map<String, Object> getContent(Long labId, Long id) throws IOException {
        RadiologyReportFormatRepository.WithContent f = repository.findContentByIdAndLabId(id, labId)
                .orElseThrow(() -> new NoSuchElementException("Report format not found"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", f.getId());
        result.put("documentName", f.getDocumentName());
        result.put("doctorId", f.getDoctorId());
        result.put("doctorName", f.getDoctorName());
        result.put("uploadedAt", f.getCreatedAt());
        result.put("content", f.getContentJson() == null ? null : MAPPER.readTree(f.getContentJson()));
        return result;
    }

    public List<Map<String, Object>> list(Long labId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (RadiologyReportFormatRepository.Summary s : repository.findByLabIdOrderByCreatedAtDesc(labId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getId());
            item.put("documentName", s.getDocumentName());
            item.put("contentType", s.getContentType());
            item.put("fileSize", s.getFileSize());
            item.put("doctorId", s.getDoctorId());
            item.put("doctorName", s.getDoctorName());
            item.put("uploadedBy", s.getUploadedBy());
            item.put("uploadedAt", s.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    // Scoped by lab so a format id from another lab is reported as not found.
    public RadiologyReportFormat get(Long labId, Long id) {
        return repository.findByIdAndLabId(id, labId)
                .orElseThrow(() -> new NoSuchElementException("Report format not found"));
    }
}
