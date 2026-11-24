package tiameds.com.tiameds.whatsapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.PatientEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VisitEntity;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.whatsapp.client.AwsWhatsAppClient;
import tiameds.com.tiameds.whatsapp.client.WhatsAppApiException;
import tiameds.com.tiameds.whatsapp.config.WhatsAppProperties;
import tiameds.com.tiameds.whatsapp.entity.WhatsAppReportLog;
import tiameds.com.tiameds.whatsapp.entity.WhatsAppSendStatus;
import tiameds.com.tiameds.whatsapp.repository.WhatsAppReportLogRepository;
import tiameds.com.tiameds.whatsapp.repository.WhatsAppVisitLookupRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sends a visit's lab report PDF to the patient on WhatsApp.
 *
 * The report PDF is produced by the frontend (the backend only stores report JSON), so the PDF arrives
 * as an upload. Flow: validate -> log PENDING -> PDF to private S3 -> PostWhatsAppMessageMedia (mediaId)
 * -> SendWhatsAppMessage (approved document-header template) -> log SENT, or FAILED with the reason.
 * Deliberately NOT @Transactional: no DB transaction is held open across network calls, and the log row
 * must survive a failure.
 */
@Service
public class WhatsAppReportService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppReportService.class);
    private static final int DUPLICATE_GUARD_SECONDS = 60;

    private final WhatsAppProperties props;
    private final AwsWhatsAppClient client;
    private final WhatsAppVisitLookupRepository visitLookup;
    private final WhatsAppReportLogRepository logRepository;
    private final LabRepository labRepository;

    public WhatsAppReportService(WhatsAppProperties props,
                                 AwsWhatsAppClient client,
                                 WhatsAppVisitLookupRepository visitLookup,
                                 WhatsAppReportLogRepository logRepository,
                                 LabRepository labRepository) {
        this.props = props;
        this.client = client;
        this.visitLookup = visitLookup;
        this.logRepository = logRepository;
        this.labRepository = labRepository;
    }

    public WhatsAppReportLog sendVisitReport(Long labId, Long visitId, MultipartFile file, User sender) {
        if (!props.isUsable()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "WhatsApp report delivery is not enabled or not fully configured");
        }
        byte[] pdf = readAndValidatePdf(file);

        VisitEntity visit = visitLookup.findVisitInLab(visitId, labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visit not found in this lab"));
        PatientEntity patient = visit.getPatient();
        String phone = normalizePhone(patient.getPhone());

        boolean recentAttempt = logRepository.existsByLabIdAndVisitIdAndStatusInAndCreatedAtAfter(
                labId, visitId, Set.of(WhatsAppSendStatus.PENDING, WhatsAppSendStatus.SENT),
                LocalDateTime.now().minusSeconds(DUPLICATE_GUARD_SECONDS));
        if (recentAttempt) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "A WhatsApp report was just sent for this visit. Please wait a minute before sending again");
        }

        String labName = labRepository.findById(labId).map(Lab::getName).orElse("Laboratory");
        String reportRef = visit.getVisitCode() != null && !visit.getVisitCode().isBlank()
                ? visit.getVisitCode() : "VISIT-" + visit.getVisitId();
        String patientName = ((patient.getFirstName() == null ? "" : patient.getFirstName()) + " "
                + (patient.getLastName() == null ? "" : patient.getLastName())).trim();
        String fileName = "Lab-Report-" + reportRef.replaceAll("[^a-zA-Z0-9._-]", "_") + ".pdf";
        String s3Key = props.getS3Prefix() + labId + "/" + visitId + "/" + UUID.randomUUID() + "-" + fileName;

        WhatsAppReportLog entry = new WhatsAppReportLog();
        entry.setLabId(labId);
        entry.setVisitId(visitId);
        entry.setPatientId(patient.getPatientId());
        entry.setPhoneNumber(phone);
        entry.setTemplateName(props.getTemplateName());
        entry.setS3Key(s3Key);
        entry.setStatus(WhatsAppSendStatus.PENDING);
        entry.setSentByUserId(sender.getId());
        entry.setSentByUsername(sender.getUsername());
        entry = logRepository.save(entry);

        try {
            client.uploadPdf(s3Key, pdf);
            String mediaId = client.postMediaFromS3(s3Key);
            entry.setMediaId(mediaId);
            String messageId = client.sendReportTemplate(phone, patientName, reportRef, labName, mediaId, fileName);
            entry.setWhatsappMessageId(messageId);
            entry.setStatus(WhatsAppSendStatus.SENT);
            entry.setSentAt(LocalDateTime.now());
            log.info("WhatsApp report sent: logId={}, labId={}, visitId={}", entry.getId(), labId, visitId);
        } catch (WhatsAppApiException e) {
            entry.setStatus(WhatsAppSendStatus.FAILED);
            entry.setErrorMessage(truncate(e.getMessage(), 1000));
            log.warn("WhatsApp report failed: logId={}, labId={}, visitId={}, reason={}",
                    entry.getId(), labId, visitId, e.getMessage());
        }
        return logRepository.save(entry);
    }

    public List<WhatsAppReportLog> history(Long labId, Long visitId) {
        visitLookup.findVisitInLab(visitId, labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visit not found in this lab"));
        return logRepository.findByLabIdAndVisitIdOrderByCreatedAtDesc(labId, visitId);
    }

    private byte[] readAndValidatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF file is required");
        }
        if (file.getSize() > props.getMaxPdfBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "PDF is too large");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }
        // check the real content, not just the client-supplied content type
        boolean looksLikePdf = bytes.length > 5 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D'
                && bytes[3] == 'F' && bytes[4] == '-';
        if (!looksLikePdf) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are allowed");
        }
        return bytes;
    }

    /** Returns the number in E.164 form (+<country><number>) or rejects it. */
    String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient has no phone number");
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (digits.length() == 10) {
            digits = props.getDefaultCountryCode() + digits;
        }
        if (digits.length() < 11 || digits.length() > 15) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient phone number is not valid for WhatsApp");
        }
        return "+" + digits;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
