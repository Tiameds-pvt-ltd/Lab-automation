package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.com.tiameds.entity.RadiologyReportFormat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RadiologyReportFormatRepository extends JpaRepository<RadiologyReportFormat, Long> {

    // Closed projection: selects only these columns, so file_data is not loaded for the list.
    interface Summary {
        Long getId();
        String getDocumentName();
        String getContentType();
        Long getFileSize();
        Long getDoctorId();
        String getDoctorName();
        String getUploadedBy();
        Instant getCreatedAt();
    }

    interface WithContent extends Summary {
        String getContentJson();
    }

    List<Summary> findByLabIdOrderByCreatedAtDesc(Long labId);

    Optional<WithContent> findContentByIdAndLabId(Long id, Long labId);

    Optional<RadiologyReportFormat> findByIdAndLabId(Long id, Long labId);
}
