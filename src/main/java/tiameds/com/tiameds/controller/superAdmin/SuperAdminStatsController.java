package tiameds.com.tiameds.controller.superAdmin;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.BillingRepository;
import tiameds.com.tiameds.repository.DoctorRepository;
import tiameds.com.tiameds.repository.HealthPackageRepository;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.PatientRepository;
import tiameds.com.tiameds.repository.TestRepository;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.repository.VisitRepository;
import tiameds.com.tiameds.repository.VisitSampleRepository;
import tiameds.com.tiameds.repository.VisitTestResultRepository;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.UserAuthService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@RestController
@RequestMapping("/lab-super-admin/stats")
@Tag(name = "Super Admin Stats Controller", description = "Statistics endpoints for super admin")
public class SuperAdminStatsController {

    private final LabRepository labRepository;
    private final PatientRepository patientRepository;
    private final VisitTestResultRepository visitTestResultRepository;
    private final UserRepository userRepository;
    private final TestRepository testRepository;
    private final BillingRepository billingRepository;
    private final VisitSampleRepository visitSampleRepository;
    private final VisitRepository visitRepository;
    private final DoctorRepository doctorRepository;
    private final HealthPackageRepository healthPackageRepository;
    private final UserAuthService userAuthService;

    public SuperAdminStatsController(LabRepository labRepository,
                                     PatientRepository patientRepository,
                                     VisitTestResultRepository visitTestResultRepository,
                                     UserRepository userRepository,
                                     TestRepository testRepository,
                                     BillingRepository billingRepository,
                                     VisitSampleRepository visitSampleRepository,
                                     VisitRepository visitRepository,
                                     DoctorRepository doctorRepository,
                                     HealthPackageRepository healthPackageRepository,
                                     UserAuthService userAuthService) {
        this.labRepository = labRepository;
        this.patientRepository = patientRepository;
        this.visitTestResultRepository = visitTestResultRepository;
        this.userRepository = userRepository;
        this.testRepository = testRepository;
        this.billingRepository = billingRepository;
        this.visitSampleRepository = visitSampleRepository;
        this.visitRepository = visitRepository;
        this.doctorRepository = doctorRepository;
        this.healthPackageRepository = healthPackageRepository;
        this.userAuthService = userAuthService;
    }

    @GetMapping("/my-labs/count")
    public ResponseEntity<?> getMyLabsCount(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        boolean hasDates = startDate != null && endDate != null;

        List<Lab> labs = labRepository.findByCreatedBy(currentUser);
        if (hasDates) {
            LocalDateTime s = toStart(startDate);
            LocalDateTime e = toEnd(endDate);
            labs = labs.stream()
                    .filter(l -> l.getCreatedAt() != null && !l.getCreatedAt().isBefore(s) && !l.getCreatedAt().isAfter(e))
                    .collect(java.util.stream.Collectors.toList());
        }

        List<Map<String, Object>> labWise = new ArrayList<>();
        for (Lab lab : labs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("labId",     lab.getId());
            row.put("labName",   lab.getName());
            row.put("createdAt", lab.getCreatedAt());
            labWise.add(row);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total",   labs.size());
        response.put("labWise", labWise);
        return ApiResponseHelper.successResponse("Total labs retrieved successfully", response);
    }

    @GetMapping("/total-admins")
    public ResponseEntity<?> getTotalAdmins(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        return ApiResponseHelper.successResponse("Total admins retrieved successfully",
                buildRoleCountWithLabWise("ADMIN", currentUser, startDate, endDate));
    }

    @GetMapping("/total-technicians")
    public ResponseEntity<?> getTotalTechnicians(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        return ApiResponseHelper.successResponse("Total technicians retrieved successfully",
                buildRoleCountWithLabWise("TECHNICIAN", currentUser, startDate, endDate));
    }

    @GetMapping("/total-deskroles")
    public ResponseEntity<?> getTotalDeskRoles(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        return ApiResponseHelper.successResponse("Total desk roles retrieved successfully",
                buildRoleCountWithLabWise("DESKROLE", currentUser, startDate, endDate));
    }

    private Map<String, Object> buildRoleCountWithLabWise(String roleName, User currentUser,
                                                           LocalDate startDate, LocalDate endDate) {
        boolean hasDates = startDate != null && endDate != null;
        List<Lab> labs = labRepository.findByCreatedBy(currentUser);

        List<Map<String, Object>> labWise = new ArrayList<>();
        long total = 0;

        for (Lab lab : labs) {
            long count = hasDates
                    ? userRepository.countByRolesNameAndLabsIdAndCreatedAtBetween(roleName, lab.getId(), toStart(startDate), toEnd(endDate))
                    : userRepository.countByRolesNameAndLabsId(roleName, lab.getId());
            total += count;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("labId",   lab.getId());
            row.put("labName", lab.getName());
            row.put("count",   count);
            labWise.add(row);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total",   total);
        response.put("labWise", labWise);
        return response;
    }

    @GetMapping("/total-tests")
    public ResponseEntity<?> getTotalTests(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        long totalTests;
        if (startDate != null && endDate != null) {
            totalTests = visitTestResultRepository.countAllTestsByLabsCreatedByAndCreatedAtBetween(currentUser, toStart(startDate), toEnd(endDate));
        } else {
            totalTests = visitTestResultRepository.countAllTestsByLabsCreatedBy(currentUser);
        }
        return ApiResponseHelper.successResponse("Total tests retrieved successfully", Map.of("totalTests", totalTests));
    }

    @GetMapping("/total-revenue")
    public ResponseEntity<?> getTotalRevenue(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        BigDecimal totalRevenue;
        if (startDate != null && endDate != null) {
            totalRevenue = billingRepository.sumPaidAmountByLabsCreatedByAndCreatedAtBetween(currentUser, toInstantStart(startDate), toInstantEnd(endDate));
        } else {
            totalRevenue = billingRepository.sumPaidAmountByLabsCreatedBy(currentUser);
        }
        return ApiResponseHelper.successResponse("Total revenue retrieved successfully", Map.of("totalRevenue", totalRevenue));
    }

    @GetMapping("/reports-generated")
    public ResponseEntity<?> getReportsGenerated(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        long reportsGenerated;
        if (startDate != null && endDate != null) {
            reportsGenerated = visitTestResultRepository.countCompletedReportsByLabsCreatedByAndCreatedAtBetween(currentUser, toStart(startDate), toEnd(endDate));
        } else {
            reportsGenerated = visitTestResultRepository.countCompletedReportsByLabsCreatedBy(currentUser);
        }
        return ApiResponseHelper.successResponse("Reports generated retrieved successfully", Map.of("reportsGenerated", reportsGenerated));
    }

    @GetMapping("/pending-samples")
    public ResponseEntity<?> getPendingSamples(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        long pendingSamples;
        if (startDate != null && endDate != null) {
            pendingSamples = visitRepository.countPendingVisitsByLabsCreatedByAndCreatedAtBetween(currentUser, toInstantStart(startDate), toInstantEnd(endDate));
        } else {
            pendingSamples = visitRepository.countPendingVisitsByLabsCreatedBy(currentUser);
        }
        return ApiResponseHelper.successResponse("Pending samples retrieved successfully", Map.of("pendingSamples", pendingSamples));
    }

    // getTestsByCategory, getRevenueTrend, getRevenueByLab moved to SuperAdminDashboardController
    // (same paths: /tests-by-category, /revenue-trend, /revenue-by-lab) — those versions add
    // labId scoping and are kept in sync with the /all endpoint's shared-data fetch helpers.
    // Having both here and there caused an ambiguous-mapping startup failure.

    @GetMapping("/{labId}/patient/{patientId}/test-summary")
    public ResponseEntity<?> getPatientTestSummary(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @PathVariable Long patientId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        try {
            Optional<User> userOptional = userAuthService.authenticateUser(token);
            if (userOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
            }

            Optional<Lab> labOptional = labRepository.findById(labId);
            if (labOptional.isEmpty()) {
                return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);
            }

            if (!patientRepository.existsByPatientIdAndLabsId(patientId, labId)) {
                return ApiResponseHelper.errorResponse("Patient not found for the specified lab", HttpStatus.NOT_FOUND);
            }

            long total, completed, pending, cancelled;
            if (startDate != null && endDate != null) {
                LocalDateTime start = toStart(startDate);
                LocalDateTime end   = toEnd(endDate);
                total     = visitTestResultRepository.countByPatientIdAndLabIdAndCreatedAtBetween(patientId, labId, start, end);
                completed = visitTestResultRepository.countByPatientIdAndLabIdAndReportStatusAndCreatedAtBetween(patientId, labId, "Completed", start, end);
                pending   = visitTestResultRepository.countByPatientIdAndLabIdAndReportStatusAndCreatedAtBetween(patientId, labId, "Pending", start, end);
                cancelled = visitTestResultRepository.countCancelledByPatientIdAndLabIdAndCreatedAtBetween(patientId, labId, start, end);
            } else {
                total     = visitTestResultRepository.countByPatientIdAndLabId(patientId, labId);
                completed = visitTestResultRepository.countByPatientIdAndLabIdAndReportStatus(patientId, labId, "Completed");
                pending   = visitTestResultRepository.countByPatientIdAndLabIdAndReportStatus(patientId, labId, "Pending");
                cancelled = visitTestResultRepository.countCancelledByPatientIdAndLabId(patientId, labId);
            }

            long partiallyCompleted = (total > 0 && completed < total) ? completed : 0;

            Map<String, Long> summary = new HashMap<>();
            summary.put("total", total);
            summary.put("completed", completed);
            summary.put("pending", pending);
            summary.put("cancelled", cancelled);
            summary.put("partiallyCompleted", partiallyCompleted);

            return ApiResponseHelper.successResponseWithDataAndMessage("Patient test summary retrieved successfully", HttpStatus.OK, summary);
        } catch (Exception e) {
            return ApiResponseHelper.errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // getLabPerformanceSummary, getTopReferringDoctors, getDetailedBilling, getPackagesSummary,
    // getDashboardSummary, getEarningsByCategory moved to SuperAdminDashboardController (same
    // paths) — those versions add labId scoping and share the /all endpoint's rollup/shared-data
    // fetch helpers. Having both here and there caused an ambiguous-mapping startup failure.

    private BigDecimal safe(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private <T> BigDecimal sumField(List<T> list, Function<T, BigDecimal> getter) {
        return list.stream()
                .map(getter)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime toStart(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime toEnd(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    private Instant toInstantStart(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Instant toInstantEnd(LocalDate date) {
        return date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();
    }
}
