package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.audit.AuditLogService;
import tiameds.com.tiameds.audit.helpers.FieldChangeTracker;
import tiameds.com.tiameds.dto.lab.PatientVisitSampleDto;
import tiameds.com.tiameds.dto.lab.VisitSampleDto;
import tiameds.com.tiameds.dto.lab.VisitTestResultResponseDTO;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.LabAuditLogs;
import tiameds.com.tiameds.entity.SampleEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VisitEntity;
import tiameds.com.tiameds.repository.SampleAssocationRepository;
import tiameds.com.tiameds.repository.VisitRepository;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;
import tiameds.com.tiameds.utils.UserAuthService;
import tiameds.com.tiameds.repository.LabRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/lab")
@Tag(name = "patient visit sample", description = "manage the patient visit sample in the lab")
public class PatientVisitSample {
    private final VisitRepository visitRepository;
    private final SampleAssocationRepository sampleAssocationRepository;
    private final UserAuthService userAuthService;
    private final LabAccessableFilter labAccessableFilter;
    private final LabRepository labRepository;
    private final AuditLogService auditLogService;
    private final FieldChangeTracker fieldChangeTracker;

    public PatientVisitSample(VisitRepository visitRepository, 
                              SampleAssocationRepository sampleAssocationRepository, 
                              UserAuthService userAuthService, 
                              LabAccessableFilter labAccessableFilter, 
                              LabRepository labRepository,
                              AuditLogService auditLogService,
                              FieldChangeTracker fieldChangeTracker) {
        this.visitRepository = visitRepository;
        this.sampleAssocationRepository = sampleAssocationRepository;
        this.userAuthService = userAuthService;
        this.labAccessableFilter = labAccessableFilter;
        this.labRepository = labRepository;
        this.auditLogService = auditLogService;
        this.fieldChangeTracker = fieldChangeTracker;
    }

    @PostMapping("/add-samples")
    @Transactional
    public ResponseEntity<?> createSampleWithPatientVisit(@RequestBody PatientVisitSampleDto request, 
                                                           @RequestHeader("Authorization") String token,
                                                           HttpServletRequest httpRequest) {
        Optional<User> currentUser = userAuthService.authenticateUser(token);
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }
        // 1. Find the visit by ID
        VisitEntity visit = visitRepository.findById(request.getVisitId()).orElse(null);
        if (visit == null) {
            return ResponseEntity.badRequest().body("Visit not found");
        }

        // Capture old state before modification
        Map<String, Object> oldData = toAuditMap(visit);

        // 2. Fetch or create samples
        Set<SampleEntity> samples = new HashSet<>();
        for (String sampleName : request.getSampleNames()) {
            SampleEntity sample = sampleAssocationRepository.findByName(sampleName).orElseGet(() -> {
                SampleEntity newSample = new SampleEntity();
                newSample.setName(sampleName);
                return sampleAssocationRepository.save(newSample); // Save new sample
            });

            samples.add(sample);
        }

        // 3. Associate samples with visit correctly
        visit.getSamples().addAll(samples);
        visitRepository.save(visit);

        //update the visit status
        visit.setVisitStatus("Collected");
        visitRepository.save(visit);

        // Capture new state after modification
        Map<String, Object> newData = toAuditMap(visit);

        // Get lab ID from visit
        Long labId = null;
        if (visit.getLabs() != null && !visit.getLabs().isEmpty()) {
            labId = Long.valueOf(visit.getLabs().iterator().next().getId());
        }

        logVisitSampleAudit(
                labId,
                "VISIT_SAMPLE_ADD",
                oldData,
                newData,
                resolveChangeReason("added", request.getSampleNames(), visit.getVisitId()),
                currentUser.get(),
                httpRequest,
                visit.getVisitId()
        );

        return ResponseEntity.ok("Samples added to visit successfully");
    }

    @PutMapping("/update-samples")
    @Transactional
    public ResponseEntity<?> updateSampleWithPatientVisit(@RequestBody PatientVisitSampleDto request, 
                                                          @RequestHeader("Authorization") String token,
                                                          HttpServletRequest httpRequest) {
        Optional<User> currentUser = userAuthService.authenticateUser(token);
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        // 1. Find the visit by ID
        VisitEntity visit = visitRepository.findById(request.getVisitId()).orElse(null);
        if (visit == null) {
            return ResponseEntity.badRequest().body("Visit not found");
        }

        // Capture old state before modification
        Map<String, Object> oldData = toAuditMap(visit);

        // 2. update the samples
        Set<SampleEntity> samples = new HashSet<>();
        for (String sampleName : request.getSampleNames()) {
            SampleEntity sample = sampleAssocationRepository.findByName(sampleName).orElseGet(() -> {
                SampleEntity newSample = new SampleEntity();
                newSample.setName(sampleName);
                return sampleAssocationRepository.save(newSample); // Save new sample
            });

            samples.add(sample);
        }

        // 3. Associate samples with visit correctly
        visit.setSamples(samples);
        visitRepository.save(visit);

        // Capture new state after modification
        Map<String, Object> newData = toAuditMap(visit);

        // Get lab ID from visit
        Long labId = null;
        if (visit.getLabs() != null && !visit.getLabs().isEmpty()) {
            labId = Long.valueOf(visit.getLabs().iterator().next().getId());
        }

        logVisitSampleAudit(
                labId,
                "VISIT_SAMPLE_UPDATE",
                oldData,
                newData,
                resolveChangeReason("updated", request.getSampleNames(), visit.getVisitId()),
                currentUser.get(),
                httpRequest,
                visit.getVisitId()
        );

        return ResponseEntity.ok("Samples updated to visit successfully");
    }

    @DeleteMapping("/delete-samples")
    @Transactional
    public ResponseEntity<?> deleteSampleWithPatientVisit(@RequestBody PatientVisitSampleDto request, 
                                                          @RequestHeader("Authorization") String token,
                                                          HttpServletRequest httpRequest) {
        Optional<User> currentUser = userAuthService.authenticateUser(token);
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }
        // 1. Find the visit by ID
        VisitEntity visit = visitRepository.findById(request.getVisitId()).orElse(null);
        if (visit == null) {
            return ResponseEntity.badRequest().body("Visit not found");
        }

        // Capture old state before modification
        Map<String, Object> oldData = toAuditMap(visit);

        // 2. Fetch or create samples
        Set<SampleEntity> samplesToRemove = new HashSet<>();
        for (String sampleName : request.getSampleNames()) {
            SampleEntity sample = sampleAssocationRepository.findByName(sampleName).orElseGet(() -> {
                SampleEntity newSample = new SampleEntity();
                newSample.setName(sampleName);
                return sampleAssocationRepository.save(newSample); // Save new sample
            });
            samplesToRemove.add(sample);
        }
        // 3. Remove samples from the visit
        visit.getSamples().removeAll(samplesToRemove);
        visitRepository.save(visit);

        //check if the visit has no samples
        if (visit.getSamples().isEmpty()) {
            visit.setVisitStatus("Pending");
            visitRepository.save(visit);  // Save the updated status
        }

        // Capture new state after modification
        Map<String, Object> newData = toAuditMap(visit);

        // Get lab ID from visit
        Long labId = null;
        if (visit.getLabs() != null && !visit.getLabs().isEmpty()) {
            labId = Long.valueOf(visit.getLabs().iterator().next().getId());
        }

        logVisitSampleAudit(
                labId,
                "VISIT_SAMPLE_DELETE",
                oldData,
                newData,
                resolveChangeReason("deleted", request.getSampleNames(), visit.getVisitId()),
                currentUser.get(),
                httpRequest,
                visit.getVisitId()
        );

        return ResponseEntity.ok("Samples deleted from visit successfully");
    }


    @GetMapping("/{labId}/patients/collected-completed")
    @Transactional
    public ResponseEntity<?> getCollectedAndCompletedPatientData(
            @PathVariable("labId") Long labId,
            @RequestHeader("Authorization") String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Optional<User> currentUser = userAuthService.authenticateUser(token);
        if (currentUser.isEmpty()) {
            return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
        }

        if (!labAccessableFilter.isLabAccessible(labId)) {
            return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
        }

        Lab lab = labRepository.findById(labId).orElse(null);
        if (lab == null) {
            return ResponseEntity.badRequest().body("Lab not found");
        }

        if (startDate == null || endDate == null) {
            return ApiResponseHelper.errorResponse("Start date and end date are required", HttpStatus.BAD_REQUEST);
        }

        List<String> visitStatus = Arrays.asList("Collected", "Completed");
        List<VisitEntity> visits = visitRepository.findAllByPatient_LabsAndVisitDateBetweenAndVisitStatusIn(
                lab, startDate, endDate, visitStatus);

        List<VisitSampleDto> visitSamples = visits.stream()
                .map(visit -> new VisitSampleDto(
                        visit.getVisitId(),
                        visit.getPatient().getFirstName() + " " + visit.getPatient().getLastName(),
                        visit.getPatient().getGender(),
                        visit.getPatient().getDateOfBirth().toString(),
                        visit.getPatient().getPhone(),
                        visit.getPatient().getEmail(),
                        visit.getVisitDate(),
                        visit.getVisitStatus(),
                        visit.getVisitType(),
                        visit.getDoctor() != null ? visit.getDoctor().getName() : null, // Handle potential null doctor
                        visit.getSamples().stream()
                                .map(SampleEntity::getName)
                                .collect(Collectors.toSet()),
                        visit.getTests().stream()
                                .map(test -> test.getId())
                                .collect(Collectors.toList()),
                        visit.getPackages().stream()
                                .map(pkg -> pkg.getId())
                                .collect(Collectors.toList()),
                        visit.getTestResults().stream()
                                .map(VisitTestResultResponseDTO::new)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return ApiResponseHelper.successResponse("Visits filtered by date and status", visitSamples);
    }

    private void logVisitSampleAudit(Long labId,
                                     String action,
                                     Map<String, Object> oldData,
                                     Map<String, Object> newData,
                                     String changeReason,
                                     User currentUser,
                                     HttpServletRequest request,
                                     Long entityId) {
        LabAuditLogs auditLog = new LabAuditLogs();
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setModule("VisitSample");
        auditLog.setEntityType("Visit");
        auditLog.setLab_id(labId != null ? String.valueOf(labId) : "GLOBAL");
        auditLog.setActionType(action);
        auditLog.setChangeReason(changeReason != null ? changeReason : "");

        if (entityId != null) {
            auditLog.setEntityId(String.valueOf(entityId));
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
        
        // Convert samples to sorted set of names for consistent comparison
        Set<String> sampleNames = visit.getSamples() != null 
                ? visit.getSamples().stream()
                    .map(SampleEntity::getName)
                    .collect(Collectors.toCollection(TreeSet::new))
                : new TreeSet<>();
        data.put("sampleNames", sampleNames);
        
        return data;
    }

    private String resolveChangeReason(String action, List<String> sampleNames, Long visitId) {
        String samplesStr = sampleNames != null && !sampleNames.isEmpty() 
                ? String.join(", ", sampleNames) 
                : "samples";
        String visitStr = visitId != null ? "visit " + visitId : "visit";
        return switch (action) {
            case "added" -> "Added samples [" + samplesStr + "] to " + visitStr;
            case "updated" -> "Updated samples to [" + samplesStr + "] for " + visitStr;
            case "deleted" -> "Deleted samples [" + samplesStr + "] from " + visitStr;
            default -> "Modified samples for " + visitStr;
        };
    }
}
