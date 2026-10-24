package tiameds.com.tiameds.services.lab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.dto.lab.RadiologyReportRequest;
import tiameds.com.tiameds.dto.lab.RadiologyTemplateRequest;
import tiameds.com.tiameds.entity.Doctors;
import tiameds.com.tiameds.entity.RadiologyReport;
import tiameds.com.tiameds.entity.RadiologyReportHistory;
import tiameds.com.tiameds.entity.RadiologyReportSignatory;
import tiameds.com.tiameds.entity.RadiologyReportStatus;
import tiameds.com.tiameds.entity.RadiologyTemplate;
import tiameds.com.tiameds.entity.VisitEntity;
import tiameds.com.tiameds.repository.DoctorRepository;
import tiameds.com.tiameds.repository.RadiologyReportHistoryRepository;
import tiameds.com.tiameds.repository.RadiologyReportRepository;
import tiameds.com.tiameds.repository.RadiologyReportSignatoryRepository;
import tiameds.com.tiameds.repository.RadiologyTemplateRepository;
import tiameds.com.tiameds.repository.TestRepository;
import tiameds.com.tiameds.repository.VisitRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * Radiology (imaging) reports: versioned templates, structured reports, signing doctors
 * and an amendment history. Kept apart from ReportService, which serves pathology results.
 *
 * Exceptions: IllegalArgumentException = bad input (400), NoSuchElementException = not
 * found (404), IllegalStateException = conflict with current state (409).
 */
@Service
public class RadiologyReportService {

    private static final String SEQUENCE_ENTITY = "RADIOLOGY_REPORT";
    private static final String SEQUENCE_PREFIX = "RAD";
    private static final int MAX_JSON_CHARS = 200_000;
    private static final int MAX_TEXT_CHARS = 5_000;
    private static final int MAX_IMPRESSION_CHARS = 10_000;
    private static final int MAX_SIGNATORIES = 5;
    private static final Set<String> SIGNATORY_ROLES = Set.of("REPORTING", "VERIFYING");

    private final RadiologyTemplateRepository templateRepository;
    private final RadiologyReportRepository reportRepository;
    private final RadiologyReportSignatoryRepository signatoryRepository;
    private final RadiologyReportHistoryRepository historyRepository;
    private final VisitRepository visitRepository;
    private final TestRepository testRepository;
    private final DoctorRepository doctorRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final ObjectMapper objectMapper;

    public RadiologyReportService(RadiologyTemplateRepository templateRepository,
                                  RadiologyReportRepository reportRepository,
                                  RadiologyReportSignatoryRepository signatoryRepository,
                                  RadiologyReportHistoryRepository historyRepository,
                                  VisitRepository visitRepository,
                                  TestRepository testRepository,
                                  DoctorRepository doctorRepository,
                                  SequenceGeneratorService sequenceGeneratorService,
                                  ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.reportRepository = reportRepository;
        this.signatoryRepository = signatoryRepository;
        this.historyRepository = historyRepository;
        this.visitRepository = visitRepository;
        this.testRepository = testRepository;
        this.doctorRepository = doctorRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------ templates

    /** Creates a new version of the template for (test, lab) and makes it the active one. */
    @Transactional
    public Map<String, Object> createTemplate(Long labId, RadiologyTemplateRequest request, String username) {
        if (request.getTestId() == null) {
            throw new IllegalArgumentException("testId is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (request.getName().trim().length() > 255) {
            throw new IllegalArgumentException("name cannot exceed 255 characters");
        }
        requireJsonObject(request.getSchemaJson(), "schemaJson");
        if (!testRepository.existsById(request.getTestId())) {
            throw new NoSuchElementException("Test not found");
        }

        boolean global = Boolean.TRUE.equals(request.getGlobal());
        int nextVersion;
        if (global) {
            nextVersion = templateRepository.findMaxVersionGlobal(request.getTestId()) + 1;
            templateRepository.deactivateActiveGlobal(request.getTestId());
        } else {
            nextVersion = templateRepository.findMaxVersionForLab(request.getTestId(), labId) + 1;
            templateRepository.deactivateActiveForLab(request.getTestId(), labId);
        }

        RadiologyTemplate template = new RadiologyTemplate();
        template.setTestId(request.getTestId());
        template.setLabId(global ? null : labId);
        template.setName(request.getName().trim());
        template.setVersion(nextVersion);
        template.setSchemaJson(request.getSchemaJson());
        template.setIsActive(true);
        template.setCreatedBy(username);
        return toTemplateResponse(templateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTemplates(Long labId, Long testId) {
        List<RadiologyTemplate> templates = testId == null
                ? templateRepository.findActiveVisibleToLab(labId)
                : templateRepository.findActiveVisibleToLabForTest(labId, testId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (RadiologyTemplate template : templates) {
            result.add(toTemplateResponse(template));
        }
        return result;
    }

    // -------------------------------------------------------------------- reports

    /**
     * Creates or updates the radiology report of a test in a visit.
     * DRAFT reports are overwritten freely. FINAL / AMENDED reports need a reason; the
     * previous content is written to the history table and the status becomes AMENDED.
     */
    @Transactional
    public Map<String, Object> saveReport(Long labId, Long visitId, Long testId,
                                          RadiologyReportRequest request, String username) {
        validateReportContent(request);
        requireVisitTestInLab(labId, visitId, testId);

        Optional<RadiologyReport> existing = reportRepository.findByLabIdAndVisitIdAndTestId(labId, visitId, testId);
        RadiologyReport report;
        boolean amendment = false;

        if (existing.isEmpty()) {
            report = new RadiologyReport();
            report.setReportCode(sequenceGeneratorService.generateCode(labId, SEQUENCE_ENTITY, SEQUENCE_PREFIX));
            report.setVisitId(visitId);
            report.setTestId(testId);
            report.setLabId(labId);
            report.setStatus(RadiologyReportStatus.DRAFT);
            report.setCreatedBy(username);
            report.setTemplateId(resolveTemplateId(labId, testId, request.getTemplateId()));
        } else {
            report = existing.get();
            if (request.getVersion() == null) {
                throw new IllegalArgumentException("version is required when updating an existing report");
            }
            checkVersion(report, request.getVersion());
            if (report.getStatus().isLocked()) {
                if (request.getReason() == null || request.getReason().isBlank()) {
                    throw new IllegalArgumentException("reason is required to change a finalized report");
                }
                if (request.getSignatories() != null && request.getSignatories().isEmpty()) {
                    throw new IllegalArgumentException("A finalized report must keep at least one signing doctor");
                }
                saveHistory(report, request.getReason().trim(), username);
                report.setStatus(RadiologyReportStatus.AMENDED);
                amendment = true;
            }
            if (request.getTemplateId() != null) {
                report.setTemplateId(resolveTemplateId(labId, testId, request.getTemplateId()));
            }
            report.setUpdatedBy(username);
        }

        report.setClinicalHistory(trimToNull(request.getClinicalHistory()));
        report.setTechnique(trimToNull(request.getTechnique()));
        report.setComparison(trimToNull(request.getComparison()));
        report.setFindingsJson(request.getFindingsJson());
        report.setImpression(trimToNull(request.getImpression()));

        RadiologyReport saved = reportRepository.saveAndFlush(report);

        if (request.getSignatories() != null) {
            replaceSignatories(saved.getId(), labId, request.getSignatories());
        } else if (amendment && signatoryRepository.findByReportIdOrderBySortOrderAsc(saved.getId()).isEmpty()) {
            throw new IllegalArgumentException("A finalized report must keep at least one signing doctor");
        }

        return toReportResponse(saved, signatoryRepository.findByReportIdOrderBySortOrderAsc(saved.getId()));
    }

    /** DRAFT -> FINAL. Checks the report is complete and signed. */
    @Transactional
    public Map<String, Object> finalizeReport(Long labId, Long visitId, Long testId, Long expectedVersion, String username) {
        RadiologyReport report = reportRepository.findByLabIdAndVisitIdAndTestId(labId, visitId, testId)
                .orElseThrow(() -> new NoSuchElementException("Radiology report not found"));
        if (report.getStatus().isLocked()) {
            throw new IllegalStateException("Report is already finalized");
        }
        checkVersion(report, expectedVersion);
        List<RadiologyReportSignatory> signatories = signatoryRepository.findByReportIdOrderBySortOrderAsc(report.getId());
        if (signatories.stream().noneMatch(s -> "REPORTING".equals(s.getRole()))) {
            throw new IllegalArgumentException("At least one REPORTING doctor is required to finalize the report");
        }

        report.setStatus(RadiologyReportStatus.FINAL);
        report.setFinalizedAt(Instant.now());
        report.setFinalizedBy(username);
        report.setUpdatedBy(username);
        return toReportResponse(reportRepository.saveAndFlush(report), signatories);
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getReport(Long labId, Long visitId, Long testId) {
        requireVisitInLab(labId, visitId);
        return reportRepository.findByLabIdAndVisitIdAndTestId(labId, visitId, testId)
                .map(r -> toReportResponse(r, signatoryRepository.findByReportIdOrderBySortOrderAsc(r.getId())));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listReportsForVisit(Long labId, Long visitId) {
        requireVisitInLab(labId, visitId);
        List<RadiologyReport> reports = reportRepository.findByLabIdAndVisitIdOrderByIdAsc(labId, visitId);
        if (reports.isEmpty()) {
            return List.of();
        }
        List<Long> ids = reports.stream().map(RadiologyReport::getId).toList();
        Map<Long, List<RadiologyReportSignatory>> byReport = new LinkedHashMap<>();
        for (RadiologyReportSignatory s : signatoryRepository.findByReportIdInOrderBySortOrderAsc(ids)) {
            byReport.computeIfAbsent(s.getReportId(), k -> new ArrayList<>()).add(s);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (RadiologyReport report : reports) {
            result.add(toReportResponse(report, byReport.getOrDefault(report.getId(), List.of())));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHistory(Long labId, Long visitId, Long testId) {
        requireVisitInLab(labId, visitId);
        RadiologyReport report = reportRepository.findByLabIdAndVisitIdAndTestId(labId, visitId, testId)
                .orElseThrow(() -> new NoSuchElementException("Radiology report not found"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (RadiologyReportHistory h : historyRepository.findByReportIdOrderByChangedAtDesc(report.getId())) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", h.getId());
            map.put("reportVersion", h.getReportVersion());
            map.put("status", h.getStatus());
            map.put("clinicalHistory", h.getClinicalHistory());
            map.put("technique", h.getTechnique());
            map.put("comparison", h.getComparison());
            map.put("findingsJson", h.getFindingsJson());
            map.put("impression", h.getImpression());
            map.put("signatories", h.getSignatoriesJson());
            map.put("changeReason", h.getChangeReason());
            map.put("changedBy", h.getChangedBy());
            map.put("changedAt", h.getChangedAt());
            result.add(map);
        }
        return result;
    }

    // -------------------------------------------------------------------- helpers

    private void validateReportContent(RadiologyReportRequest request) {
        requireJsonObject(request.getFindingsJson(), "findingsJson");
        requireMaxLength(request.getClinicalHistory(), MAX_TEXT_CHARS, "clinicalHistory");
        requireMaxLength(request.getTechnique(), MAX_TEXT_CHARS, "technique");
        requireMaxLength(request.getComparison(), MAX_TEXT_CHARS, "comparison");
        requireMaxLength(request.getImpression(), MAX_IMPRESSION_CHARS, "impression");
        requireMaxLength(request.getReason(), MAX_TEXT_CHARS, "reason");
    }

    private void requireJsonObject(JsonNode node, String field) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException(field + " is required and must be a JSON object");
        }
        if (node.toString().length() > MAX_JSON_CHARS) {
            throw new IllegalArgumentException(field + " is too large");
        }
    }

    private void requireMaxLength(String value, int max, String field) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException(field + " cannot exceed " + max + " characters");
        }
    }

    private VisitEntity requireVisitInLab(Long labId, Long visitId) {
        VisitEntity visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new NoSuchElementException("Visit not found"));
        boolean visitInLab = visit.getLabs().stream().anyMatch(l -> l.getId() == labId);
        if (!visitInLab) {
            throw new NoSuchElementException("Visit not found");
        }
        return visit;
    }

    // The test must have been ordered on this visit, directly or through a health package.
    private void requireVisitTestInLab(Long labId, Long visitId, Long testId) {
        VisitEntity visit = requireVisitInLab(labId, visitId);
        boolean testInVisit = visit.getTests().stream().anyMatch(t -> t.getId() == testId)
                || visit.getPackages().stream()
                .anyMatch(p -> p.getTests().stream().anyMatch(t -> t.getId() == testId));
        if (!testInVisit) {
            throw new IllegalArgumentException("Test is not part of this visit");
        }
    }

    // Explicit template wins (must match the test and be visible to the lab); otherwise
    // the lab's own active template, else the global one, else null (free-form report).
    private Long resolveTemplateId(Long labId, Long testId, Long requestedTemplateId) {
        if (requestedTemplateId != null) {
            RadiologyTemplate template = templateRepository.findById(requestedTemplateId)
                    .orElseThrow(() -> new IllegalArgumentException("Template not found"));
            boolean visibleToLab = template.getLabId() == null || template.getLabId().equals(labId);
            if (!template.getTestId().equals(testId) || !visibleToLab) {
                throw new IllegalArgumentException("Template does not belong to this test");
            }
            return template.getId();
        }
        return templateRepository.findByTestIdAndLabIdAndIsActiveTrue(testId, labId)
                .or(() -> templateRepository.findByTestIdAndLabIdIsNullAndIsActiveTrue(testId))
                .map(RadiologyTemplate::getId)
                .orElse(null);
    }

    private void checkVersion(RadiologyReport report, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(report.getVersion())) {
            throw new IllegalStateException("Report was changed by someone else. Reload it and try again.");
        }
    }

    private void replaceSignatories(Long reportId, Long labId, List<RadiologyReportRequest.Signatory> requested) {
        if (requested.size() > MAX_SIGNATORIES) {
            throw new IllegalArgumentException("A report can have at most " + MAX_SIGNATORIES + " signing doctors");
        }
        Set<Long> seen = new HashSet<>();
        List<RadiologyReportSignatory> toSave = new ArrayList<>();
        int order = 0;
        for (RadiologyReportRequest.Signatory item : requested) {
            if (item == null || item.getDoctorId() == null) {
                throw new IllegalArgumentException("doctorId is required for every signatory");
            }
            if (!seen.add(item.getDoctorId())) {
                throw new IllegalArgumentException("The same doctor cannot sign twice");
            }
            String role = item.getRole() == null || item.getRole().isBlank()
                    ? "REPORTING" : item.getRole().trim().toUpperCase();
            if (!SIGNATORY_ROLES.contains(role)) {
                throw new IllegalArgumentException("role must be REPORTING or VERIFYING");
            }
            Doctors doctor = doctorRepository.findById(item.getDoctorId())
                    .orElseThrow(() -> new IllegalArgumentException("Doctor " + item.getDoctorId() + " not found"));
            if (!signatoryRepository.isDoctorInLab(labId, doctor.getId())) {
                throw new IllegalArgumentException("Doctor " + doctor.getId() + " does not belong to this lab");
            }
            RadiologyReportSignatory signatory = new RadiologyReportSignatory();
            signatory.setReportId(reportId);
            signatory.setDoctorId(doctor.getId());
            signatory.setRole(role);
            signatory.setSortOrder(order++);
            signatory.setDoctorName(doctor.getName());
            signatory.setDesignation(doctor.getSpeciality());
            signatory.setQualification(doctor.getQualification());
            signatory.setLicenseNumber(doctor.getLicenseNumber());
            toSave.add(signatory);
        }
        signatoryRepository.deleteAllByReportId(reportId);
        signatoryRepository.saveAll(toSave);
    }

    // Copies the report as it is RIGHT NOW (before the change is applied) into the history.
    private void saveHistory(RadiologyReport report, String reason, String username) {
        RadiologyReportHistory history = new RadiologyReportHistory();
        history.setReportId(report.getId());
        history.setReportVersion(report.getVersion());
        history.setStatus(report.getStatus());
        history.setClinicalHistory(report.getClinicalHistory());
        history.setTechnique(report.getTechnique());
        history.setComparison(report.getComparison());
        history.setFindingsJson(report.getFindingsJson());
        history.setImpression(report.getImpression());
        history.setSignatoriesJson(signatoriesToJson(signatoryRepository.findByReportIdOrderBySortOrderAsc(report.getId())));
        history.setChangeReason(reason);
        history.setChangedBy(username);
        historyRepository.save(history);
    }

    private JsonNode signatoriesToJson(List<RadiologyReportSignatory> signatories) {
        ArrayNode array = objectMapper.createArrayNode();
        for (RadiologyReportSignatory s : signatories) {
            ObjectNode node = array.addObject();
            node.put("doctorId", s.getDoctorId());
            node.put("role", s.getRole());
            node.put("doctorName", s.getDoctorName());
            node.put("designation", s.getDesignation());
            node.put("qualification", s.getQualification());
            node.put("licenseNumber", s.getLicenseNumber());
        }
        return array;
    }

    private Map<String, Object> toTemplateResponse(RadiologyTemplate template) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", template.getId());
        map.put("testId", template.getTestId());
        map.put("labId", template.getLabId());
        map.put("global", template.getLabId() == null);
        map.put("name", template.getName());
        map.put("version", template.getVersion());
        map.put("schemaJson", template.getSchemaJson());
        map.put("isActive", template.getIsActive());
        map.put("createdBy", template.getCreatedBy());
        map.put("createdAt", template.getCreatedAt());
        return map;
    }

    private Map<String, Object> toReportResponse(RadiologyReport report, List<RadiologyReportSignatory> signatories) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", report.getId());
        map.put("reportCode", report.getReportCode());
        map.put("visitId", report.getVisitId());
        map.put("testId", report.getTestId());
        map.put("labId", report.getLabId());
        map.put("templateId", report.getTemplateId());
        map.put("status", report.getStatus());
        map.put("clinicalHistory", report.getClinicalHistory());
        map.put("technique", report.getTechnique());
        map.put("comparison", report.getComparison());
        map.put("findingsJson", report.getFindingsJson());
        map.put("impression", report.getImpression());
        List<Map<String, Object>> signatoryList = new ArrayList<>();
        for (RadiologyReportSignatory s : signatories) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("doctorId", s.getDoctorId());
            sm.put("role", s.getRole());
            sm.put("sortOrder", s.getSortOrder());
            sm.put("doctorName", s.getDoctorName());
            sm.put("designation", s.getDesignation());
            sm.put("qualification", s.getQualification());
            sm.put("licenseNumber", s.getLicenseNumber());
            signatoryList.add(sm);
        }
        map.put("signatories", signatoryList);
        map.put("finalizedAt", report.getFinalizedAt());
        map.put("finalizedBy", report.getFinalizedBy());
        map.put("createdBy", report.getCreatedBy());
        map.put("updatedBy", report.getUpdatedBy());
        map.put("createdAt", report.getCreatedAt());
        map.put("updatedAt", report.getUpdatedAt());
        map.put("version", report.getVersion());
        return map;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
