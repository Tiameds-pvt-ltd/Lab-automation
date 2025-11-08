package tiameds.com.tiameds.controller.sampleassociation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.audit.AuditLogService;
import tiameds.com.tiameds.audit.helpers.FieldChangeTracker;
import tiameds.com.tiameds.dto.lab.ReportDto;
import tiameds.com.tiameds.dto.lab.ReportRequestDto;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.LabAuditLogs;
import tiameds.com.tiameds.entity.PatientEntity;
import tiameds.com.tiameds.entity.ReportEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VisitEntity;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.ReportRepository;
import tiameds.com.tiameds.repository.VisitRepository;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;
import tiameds.com.tiameds.services.lab.ReportService;
import java.time.LocalDateTime;
import java.util.*;


@RestController
@RequestMapping("/lab")
@Tag(name = "Report Generation", description = "manage the report generation in the lab")
public class ReportGeneration {

    private final UserAuthService userAuthService;
    private final ReportService reportService;
    private final LabAccessableFilter labAccessableFilter;
    private final LabRepository labRepository;
    private final AuditLogService auditLogService;
    private final FieldChangeTracker fieldChangeTracker;
    private final VisitRepository visitRepository;
    private final ReportRepository reportRepository;

    public ReportGeneration(UserAuthService userAuthService, 
                           ReportService reportService, 
                           LabAccessableFilter labAccessableFilter, 
                           LabRepository labRepository,
                           AuditLogService auditLogService,
                           FieldChangeTracker fieldChangeTracker,
                           VisitRepository visitRepository,
                           ReportRepository reportRepository) {
        this.userAuthService = userAuthService;
        this.reportService = reportService;
        this.labAccessableFilter = labAccessableFilter;
        this.labRepository = labRepository;
        this.auditLogService = auditLogService;
        this.fieldChangeTracker = fieldChangeTracker;
        this.visitRepository = visitRepository;
        this.reportRepository = reportRepository;
    }

//    @Transactional
//    @PostMapping("{labId}/report")
//    public ResponseEntity<?> createReport(
//            @PathVariable Long labId,
//            @RequestBody List<ReportDto> reportDtoList,
//            @RequestHeader("Authorization") String token) {
//
//        Optional<User> currentUser = userAuthService.authenticateUser(token);
//        if (currentUser.isEmpty()) {
//            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
//        }
//        Optional<Lab> lab = labRepository.findById(labId);
//        if (lab.isEmpty()) {
//            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
//        }
//        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
//        if (!isAccessible) {
//            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
//        }
//        // Check if the user is a member of the lab
//        if (!currentUser.get().getLabs().contains(lab.get())) {
//            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
//        }
//        return reportService.createReports(reportDtoList, labId, currentUser.get());
//    }


    @PutMapping("{labId}/complete-visit/{visitId}")
    public ResponseEntity<?> completeVisit(
            @PathVariable Long labId,
            @PathVariable Long visitId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {
        Optional<User> currentUser = userAuthService.authenticateUser(token);
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }
        if (!labAccessableFilter.isLabAccessible(labId)) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        Optional<Lab> lab = labRepository.findById(labId);
        if (lab.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        // Check if the user is a member of the lab
//        if (!currentUser.get().getLabs().contains(lab.get())) {
//            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
//        }

        // Ensure visit ID is valid
        if (visitId == null) {
            return ApiResponseHelper.errorResponse("Visit ID cannot be empty", HttpStatus.BAD_REQUEST);
        }

        // Capture old state before modification
        Optional<VisitEntity> oldVisitOpt = visitRepository.findById(visitId);
        if (oldVisitOpt.isEmpty()) {
            return ApiResponseHelper.errorResponse("Visit not found", HttpStatus.NOT_FOUND);
        }
        Map<String, Object> oldData = toAuditMap(oldVisitOpt.get());

        // Call the service to complete the visit
        ResponseEntity<?> response = reportService.completeVisit(visitId);

        // Capture new state after modification
        Optional<VisitEntity> newVisitOpt = visitRepository.findById(visitId);
        if (newVisitOpt.isPresent()) {
            Map<String, Object> newData = toAuditMap(newVisitOpt.get());

            logReportAudit(
                    labId,
                    "VISIT_COMPLETE",
                    oldData,
                    newData,
                    "Completed visit " + visitId,
                    currentUser.get(),
                    request,
                    String.valueOf(visitId)
            );
        }

        return response;
    }


    @PutMapping("{labId}/cancled-visit/{visitId}")
    public ResponseEntity<?> canceldVisit(
            @PathVariable Long labId,
            @PathVariable Long visitId,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {
        Optional<User> currentUser = userAuthService.authenticateUser(token);
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }
        if (!labAccessableFilter.isLabAccessible(labId)) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        Optional<Lab> lab = labRepository.findById(labId);
        if (lab.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        // Ensure visit ID is valid
        if (visitId == null) {
            return ApiResponseHelper.errorResponse("Visit ID cannot be empty", HttpStatus.BAD_REQUEST);
        }

        // Capture old state before modification
        Optional<VisitEntity> oldVisitOpt = visitRepository.findById(visitId);
        if (oldVisitOpt.isEmpty()) {
            return ApiResponseHelper.errorResponse("Visit not found", HttpStatus.NOT_FOUND);
        }
        Map<String, Object> oldData = toAuditMap(oldVisitOpt.get());

        // Call the service to cancel the visit
        ResponseEntity<?> response = reportService.canceledVisit(visitId);

        // Capture new state after modification
        Optional<VisitEntity> newVisitOpt = visitRepository.findById(visitId);
        if (newVisitOpt.isPresent()) {
            Map<String, Object> newData = toAuditMap(newVisitOpt.get());

            logReportAudit(
                    labId,
                    "VISIT_CANCEL",
                    oldData,
                    newData,
                    "Canceled visit " + visitId,
                    currentUser.get(),
                    request,
                    String.valueOf(visitId)
            );
        }

        return response;
    }



    @Transactional
    @GetMapping("{labId}/report/{visitId}")
    public ResponseEntity<?> getReport(
            @PathVariable Long labId,
            @PathVariable Long visitId,
            @RequestHeader("Authorization") String token) {

        Optional<User> currentUser = userAuthService.authenticateUser(token);
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }
        Optional<Lab> lab = labRepository.findById(labId);
        if (lab.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }
        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (!isAccessible) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        if (!currentUser.get().getLabs().contains(lab.get())) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Ensure report list is valid
        if (visitId == null) {
            return ApiResponseHelper.errorResponse("Visit ID cannot be empty", HttpStatus.BAD_REQUEST);
        }
        return reportService.getReport(visitId, labId);
    }

    @Transactional
    @PutMapping("{labId}/report")
    public ResponseEntity<?> updateReports(
            @PathVariable Long labId,
            @RequestBody List<ReportDto> reportDtoList,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        Optional<User> currentUser = userAuthService.authenticateUser(token);
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        Optional<Lab> lab = labRepository.findById(labId);
        if (lab.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        if (!currentUser.get().getLabs().contains(lab.get())) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        if (reportDtoList == null || reportDtoList.isEmpty()) {
            return ApiResponseHelper.errorResponse("Report list cannot be empty", HttpStatus.BAD_REQUEST);
        }

        // Capture old state before modification
        List<Map<String, Object>> oldReports = new ArrayList<>();
        for (ReportDto reportDto : reportDtoList) {
            if (reportDto.getReportId() != null) {
                Optional<ReportEntity> oldReportOpt = reportRepository.findById(reportDto.getReportId());
                if (oldReportOpt.isPresent()) {
                    oldReports.add(toReportAuditMap(oldReportOpt.get()));
                }
            }
        }

        // Call the service to update reports
        ResponseEntity<?> response = reportService.updateReports(reportDtoList, currentUser.get());

        // Capture new state after modification
        List<Map<String, Object>> newReports = new ArrayList<>();
        for (ReportDto reportDto : reportDtoList) {
            if (reportDto.getReportId() != null) {
                Optional<ReportEntity> newReportOpt = reportRepository.findById(reportDto.getReportId());
                if (newReportOpt.isPresent()) {
                    newReports.add(toReportAuditMap(newReportOpt.get()));
                }
            }
        }

        // Log audit for each report updated
        for (int i = 0; i < reportDtoList.size(); i++) {
            ReportDto reportDto = reportDtoList.get(i);
            Map<String, Object> oldData = i < oldReports.size() ? oldReports.get(i) : null;
            Map<String, Object> newData = i < newReports.size() ? newReports.get(i) : null;

            logReportAudit(
                    labId,
                    "REPORT_UPDATE",
                    oldData,
                    newData,
                    "Updated report " + (reportDto.getReportId() != null ? reportDto.getReportId() : "new"),
                    currentUser.get(),
                    request,
                    reportDto.getReportId() != null ? String.valueOf(reportDto.getReportId()) : null
            );
        }

        return response;
    }



    @Transactional
    @PostMapping("{labId}/report")
    public ResponseEntity<?> createReport(
            @PathVariable Long labId,
            @RequestBody ReportRequestDto reportRequestDto,
            @RequestHeader("Authorization") String token,
            HttpServletRequest request) {

        Optional<User> currentUser = userAuthService.authenticateUser(token);
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        Optional<Lab> lab = labRepository.findById(labId);
        if (lab.isEmpty()) {
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
        }

        boolean isAccessible = labAccessableFilter.isLabAccessible(labId);
        if (!isAccessible) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        if (!currentUser.get().getLabs().contains(lab.get())) {
            return ApiResponseHelper.errorResponse("User is not a member of this lab", HttpStatus.UNAUTHORIZED);
        }

        // Capture old state before modification (visit state and existing reports)
        List<Map<String, Object>> oldDataList = new ArrayList<>();
        if (reportRequestDto.getTestData() != null && !reportRequestDto.getTestData().isEmpty()) {
            Long visitId = reportRequestDto.getTestData().get(0).getVisitId();
            if (visitId != null) {
                Optional<VisitEntity> oldVisitOpt = visitRepository.findById(visitId);
                if (oldVisitOpt.isPresent()) {
                    oldDataList.add(toAuditMap(oldVisitOpt.get()));
                }
                // Capture existing reports for this visit
                List<ReportEntity> existingReports = reportRepository.findByVisitIdAndLabId(visitId, labId);
                for (ReportEntity report : existingReports) {
                    oldDataList.add(toReportAuditMap(report));
                }
            }
        }

        // Call the service to create reports
        ResponseEntity<?> response = reportService.createReports(
                reportRequestDto.getTestData(),
                labId,
                currentUser.get(),
                reportRequestDto.getTestResult()
        );

        // Capture new state after modification
        List<Map<String, Object>> newDataList = new ArrayList<>();
        if (reportRequestDto.getTestData() != null && !reportRequestDto.getTestData().isEmpty()) {
            Long visitId = reportRequestDto.getTestData().get(0).getVisitId();
            if (visitId != null) {
                Optional<VisitEntity> newVisitOpt = visitRepository.findById(visitId);
                if (newVisitOpt.isPresent()) {
                    newDataList.add(toAuditMap(newVisitOpt.get()));
                }
                // Capture newly created reports
                List<ReportEntity> newReports = reportRepository.findByVisitIdAndLabId(visitId, labId);
                for (ReportEntity report : newReports) {
                    newDataList.add(toReportAuditMap(report));
                }
            }
        }

        // Log audit for report creation
        if (!reportRequestDto.getTestData().isEmpty()) {
            Long visitId = reportRequestDto.getTestData().get(0).getVisitId();
            Map<String, Object> oldData = oldDataList.isEmpty() ? null : oldDataList.get(0);
            Map<String, Object> newData = newDataList.isEmpty() ? null : newDataList.get(0);

            logReportAudit(
                    labId,
                    "REPORT_CREATE",
                    oldData,
                    newData,
                    "Created reports for visit " + (visitId != null ? visitId : "unknown"),
                    currentUser.get(),
                    request,
                    visitId != null ? String.valueOf(visitId) : null
            );
        }

        return response;
    }

    private void logReportAudit(Long labId,
                               String action,
                               Map<String, Object> oldData,
                               Map<String, Object> newData,
                               String changeReason,
                               User currentUser,
                               HttpServletRequest request,
                               String entityId) {
        LabAuditLogs auditLog = new LabAuditLogs();
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setModule("ReportGeneration");
        auditLog.setEntityType("Report");
        auditLog.setLab_id(labId != null ? String.valueOf(labId) : "GLOBAL");
        auditLog.setActionType(action);
        auditLog.setChangeReason(changeReason != null ? changeReason : "");

        if (entityId != null) {
            auditLog.setEntityId(entityId);
        }

        if (currentUser != null) {
            auditLog.setUsername(currentUser.getUsername());
            auditLog.setUserId(currentUser.getId());
            if (currentUser.getRoles() != null && !currentUser.getRoles().isEmpty()) {
                auditLog.setRole(currentUser.getRoles().iterator().next().getName());
            }
        } else {
            auditLog.setUsername("system");
        }

        if (request != null) {
            String ipAddress = request.getHeader("X-Forwarded-For");
            auditLog.setIpAddress(ipAddress != null ? ipAddress : request.getRemoteAddr());
            auditLog.setDeviceInfo(request.getHeader("User-Agent"));
            auditLog.setRequestId(request.getHeader("X-Request-ID"));
        }

        auditLog.setOldValue(fieldChangeTracker.objectToJson(oldData));
        auditLog.setNewValue(fieldChangeTracker.objectToJson(newData));

        if (oldData != null || newData != null) {
            Map<String, Object> fieldChanges = fieldChangeTracker.compareMaps(oldData, newData);
            String fieldChangedJson = fieldChangeTracker.fieldChangesToJson(fieldChanges);
            if (fieldChangedJson != null) {
                auditLog.setFieldChanged(fieldChangedJson);
            }
        }

        auditLog.setSeverity(LabAuditLogs.Severity.MEDIUM);
        auditLogService.persistAsync(auditLog);
    }

    private Map<String, Object> toAuditMap(VisitEntity visit) {
        if (visit == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("visitId", visit.getVisitId());
        data.put("visitStatus", visit.getVisitStatus());
        data.put("visitDate", visit.getVisitDate() != null ? visit.getVisitDate().toString() : null);
        data.put("visitType", visit.getVisitType());
        data.put("visitDescription", visit.getVisitDescription());
        if (visit.getPatient() != null) {
            data.put("patientId", visit.getPatient().getPatientId());
            data.put("patientName", visit.getPatient().getFirstName() + " " + visit.getPatient().getLastName());
        }
        return data;
    }

    private Map<String, Object> toReportAuditMap(ReportEntity report) {
        if (report == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        
        // Report details
        data.put("reportId", report.getReportId());
        data.put("visitId", report.getVisitId());
        data.put("testName", report.getTestName());
        data.put("testCategory", report.getTestCategory());
        data.put("patientName", report.getPatientName());
        data.put("labId", report.getLabId());
        data.put("referenceDescription", report.getReferenceDescription());
        data.put("referenceRange", report.getReferenceRange());
        data.put("referenceAgeRange", report.getReferenceAgeRange());
        data.put("enteredValue", report.getEnteredValue());
        data.put("unit", report.getUnit());
        data.put("description", report.getDescription());
        data.put("remarks", report.getRemarks());
        data.put("comments", report.getComments());
        data.put("reportJson", report.getReportJson());
        data.put("referenceRanges", report.getReferenceRanges());
        data.put("createdBy", report.getCreatedBy());
        data.put("updatedBy", report.getUpdatedBy());
        data.put("createdAt", report.getCreatedAt() != null ? report.getCreatedAt().toString() : null);
        data.put("updatedAt", report.getUpdatedAt() != null ? report.getUpdatedAt().toString() : null);
        
        // Fetch and include full patient details from visit
        if (report.getVisitId() != null) {
            Optional<VisitEntity> visitOpt = visitRepository.findById(report.getVisitId());
            if (visitOpt.isPresent() && visitOpt.get().getPatient() != null) {
                PatientEntity patient = visitOpt.get().getPatient();
                Map<String, Object> patientDetails = new LinkedHashMap<>();
                patientDetails.put("patientId", patient.getPatientId());
                patientDetails.put("firstName", patient.getFirstName());
                patientDetails.put("lastName", patient.getLastName());
                patientDetails.put("email", patient.getEmail());
                patientDetails.put("phone", patient.getPhone());
                patientDetails.put("address", patient.getAddress());
                patientDetails.put("city", patient.getCity());
                patientDetails.put("state", patient.getState());
                patientDetails.put("zip", patient.getZip());
                patientDetails.put("bloodGroup", patient.getBloodGroup());
                patientDetails.put("dateOfBirth", patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : null);
                patientDetails.put("age", patient.getAge());
                patientDetails.put("gender", patient.getGender());
                patientDetails.put("patientCode", patient.getPatientCode());
                data.put("patient", patientDetails);
            }
        }
        
        return data;
    }
}
