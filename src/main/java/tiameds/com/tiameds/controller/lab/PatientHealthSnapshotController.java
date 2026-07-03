package tiameds.com.tiameds.controller.lab;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.entity.PatientEntity;
import tiameds.com.tiameds.entity.ReportEntity;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.entity.VisitEntity;
import tiameds.com.tiameds.repository.PatientRepository;
import tiameds.com.tiameds.repository.ReportRepository;
import tiameds.com.tiameds.repository.VisitRepository;
import tiameds.com.tiameds.services.auth.MyUserDetails;
import tiameds.com.tiameds.services.auth.UserService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.LabAccessableFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/lab")
@Tag(name = "Patient Health Snapshot Controller", description = "Health snapshot of a patient across all visits")
public class PatientHealthSnapshotController {

    private final ReportRepository reportRepository;
    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;
    private final LabAccessableFilter labAccessableFilter;
    private final UserService userService;

    public PatientHealthSnapshotController(ReportRepository reportRepository,
                                           PatientRepository patientRepository,
                                           VisitRepository visitRepository,
                                           LabAccessableFilter labAccessableFilter,
                                           UserService userService) {
        this.reportRepository = reportRepository;
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.labAccessableFilter = labAccessableFilter;
        this.userService = userService;
    }

    @GetMapping("/{labId}/patient/{patientId}/health-snapshot")
    public ResponseEntity<?> getHealthSnapshot(
            @PathVariable Long labId,
            @PathVariable Long patientId) {
        try {
            Optional<User> currentUser = getAuthenticatedUser();
            if (currentUser.isEmpty()) {
                return ApiResponseHelper.errorResponse("User not found", HttpStatus.UNAUTHORIZED);
            }

            if (!labAccessableFilter.isLabAccessible(labId)) {
                return ApiResponseHelper.errorResponse("Lab is not accessible", HttpStatus.UNAUTHORIZED);
            }

            Optional<PatientEntity> patientOptional = patientRepository.findById(patientId);
            if (patientOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Patient not found", HttpStatus.NOT_FOUND);
            }

            if (!patientRepository.existsByPatientIdAndLabsId(patientId, labId)) {
                return ApiResponseHelper.errorResponse("Patient does not belong to this lab", HttpStatus.NOT_FOUND);
            }

            PatientEntity patient = patientOptional.get();
            List<ReportEntity> reports = reportRepository.findByPatientIdAndLabId(patientId, labId);

            // Build a visit cache to avoid repeated DB calls
            Map<Long, VisitEntity> visitCache = new HashMap<>();

            // Group reports by testName (preserving insertion order = alphabetical from query)
            Map<String, List<Map<String, Object>>> groupedByTest = new LinkedHashMap<>();

            for (ReportEntity report : reports) {
                String testName = report.getTestName();
                groupedByTest.computeIfAbsent(testName, k -> new ArrayList<>());

                VisitEntity visit = visitCache.computeIfAbsent(report.getVisitId(),
                        id -> visitRepository.findById(id).orElse(null));

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("reportId", report.getReportId());
                result.put("reportCode", report.getReportCode());
                result.put("visitId", report.getVisitId());
                result.put("visitCode", visit != null ? visit.getVisitCode() : null);
                result.put("visitDate", visit != null ? visit.getVisitDate() : null);
                result.put("enteredValue", report.getEnteredValue());
                result.put("unit", report.getUnit());
                result.put("referenceRange", report.getReferenceRange());
                result.put("referenceAgeRange", report.getReferenceAgeRange());
                result.put("testRows", report.getTestRows());
                result.put("createdAt", report.getCreatedAt());

                groupedByTest.get(testName).add(result);
            }

            // Build final test list
            List<Map<String, Object>> tests = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : groupedByTest.entrySet()) {
                // Derive category from first report of this test
                String category = reports.stream()
                        .filter(r -> entry.getKey().equals(r.getTestName()))
                        .findFirst()
                        .map(ReportEntity::getTestCategory)
                        .orElse(null);

                Map<String, Object> testGroup = new LinkedHashMap<>();
                testGroup.put("testName", entry.getKey());
                testGroup.put("testCategory", category);
                testGroup.put("totalVisits", entry.getValue().size());
                testGroup.put("results", entry.getValue());
                tests.add(testGroup);
            }

            Map<String, Object> patientInfo = new LinkedHashMap<>();
            patientInfo.put("patientId", patient.getPatientId());
            patientInfo.put("patientCode", patient.getPatientCode());
            patientInfo.put("firstName", patient.getFirstName());
            patientInfo.put("lastName", patient.getLastName());
            patientInfo.put("age", patient.getAge());
            patientInfo.put("gender", patient.getGender());
            patientInfo.put("bloodGroup", patient.getBloodGroup());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("patient", patientInfo);
            response.put("totalTests", tests.size());
            response.put("tests", tests);

            return ApiResponseHelper.successResponse("Health snapshot retrieved successfully", response);

        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof MyUserDetails myUserDetails) {
            return userService.findByUsername(myUserDetails.getUsername());
        }
        if (principal instanceof UserDetails userDetails) {
            return userService.findByUsername(userDetails.getUsername());
        }
        if (principal instanceof String username && !"anonymousUser".equalsIgnoreCase(username)) {
            return userService.findByUsername(username);
        }
        return Optional.empty();
    }
}
