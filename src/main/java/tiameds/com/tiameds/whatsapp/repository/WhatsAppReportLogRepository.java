package tiameds.com.tiameds.whatsapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tiameds.com.tiameds.whatsapp.entity.WhatsAppReportLog;
import tiameds.com.tiameds.whatsapp.entity.WhatsAppSendStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WhatsAppReportLogRepository extends JpaRepository<WhatsAppReportLog, Long> {

    List<WhatsAppReportLog> findByLabIdAndVisitIdOrderByCreatedAtDesc(Long labId, Long visitId);

    /** Double-click / retry guard: is there a recent attempt for this visit? */
    boolean existsByLabIdAndVisitIdAndStatusInAndCreatedAtAfter(
            Long labId, Long visitId, Collection<WhatsAppSendStatus> statuses, LocalDateTime after);

    /** For attaching delivery receipts (webhook) later. */
    Optional<WhatsAppReportLog> findFirstByWhatsappMessageId(String whatsappMessageId);
}
