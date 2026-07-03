package tiameds.com.tiameds.controller.superAdmin;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.BillingRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final UserAuthService userAuthService;

    public SuperAdminStatsController(LabRepository labRepository,
                                     PatientRepository patientRepository,
                                     VisitTestResultRepository visitTestResultRepository,
                                     UserRepository userRepository,
                                     TestRepository testRepository,
                                     BillingRepository billingRepository,
                                     VisitSampleRepository visitSampleRepository,
                                     VisitRepository visitRepository,
                                     UserAuthService userAuthService) {
        this.labRepository = labRepository;
        this.patientRepository = patientRepository;
        this.visitTestResultRepository = visitTestResultRepository;
        this.userRepository = userRepository;
        this.testRepository = testRepository;
        this.billingRepository = billingRepository;
        this.visitSampleRepository = visitSampleRepository;
        this.visitRepository = visitRepository;
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
        long totalLabs;
        if (startDate != null && endDate != null) {
            totalLabs = labRepository.countByCreatedByAndCreatedAtBetween(currentUser, toStart(startDate), toEnd(endDate));
        } else {
            totalLabs = labRepository.countByCreatedBy(currentUser);
        }
        return ApiResponseHelper.successResponse("Total labs retrieved successfully", Map.of("totalLabs", totalLabs));
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
        long totalAdmins;
        if (startDate != null && endDate != null) {
            totalAdmins = userRepository.countByRolesNameAndCreatedByAndCreatedAtBetween("ADMIN", currentUser, toStart(startDate), toEnd(endDate));
        } else {
            totalAdmins = userRepository.countByRolesNameAndCreatedBy("ADMIN", currentUser);
        }
        return ApiResponseHelper.successResponse("Total admins retrieved successfully", Map.of("totalAdmins", totalAdmins));
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
        long totalTechnicians;
        if (startDate != null && endDate != null) {
            totalTechnicians = userRepository.countByRolesNameAndCreatedByAndCreatedAtBetween("TECHNICIAN", currentUser, toStart(startDate), toEnd(endDate));
        } else {
            totalTechnicians = userRepository.countByRolesNameAndCreatedBy("TECHNICIAN", currentUser);
        }
        return ApiResponseHelper.successResponse("Total technicians retrieved successfully", Map.of("totalTechnicians", totalTechnicians));
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
        long totalDeskRoles;
        if (startDate != null && endDate != null) {
            totalDeskRoles = userRepository.countByRolesNameAndCreatedByAndCreatedAtBetween("DESKROLE", currentUser, toStart(startDate), toEnd(endDate));
        } else {
            totalDeskRoles = userRepository.countByRolesNameAndCreatedBy("DESKROLE", currentUser);
        }
        return ApiResponseHelper.successResponse("Total desk roles retrieved successfully", Map.of("totalDeskRoles", totalDeskRoles));
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
            totalTests = testRepository.countByLabsCreatedByAndCreatedAtBetween(currentUser, toStart(startDate), toEnd(endDate));
        } else {
            totalTests = testRepository.countByLabsCreatedBy(currentUser);
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
            totalRevenue = billingRepository.sumTotalRevenueByLabsCreatedByAndCreatedAtBetween(currentUser, toInstantStart(startDate), toInstantEnd(endDate));
        } else {
            totalRevenue = billingRepository.sumTotalRevenueByLabsCreatedBy(currentUser);
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

    @GetMapping("/revenue-trend")
    public ResponseEntity<?> getRevenueTrend(
            @RequestHeader("Authorization") String token,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        Instant start = toInstantStart(startDate);
        Instant end   = toInstantEnd(endDate);

        List<BillingRepository.DailyRevenueProjection> trend =
                billingRepository.getDailyRevenueTrend(currentUser.getId(), start, end);

        BigDecimal totalRevenue = trend.stream()
                .map(BillingRepository.DailyRevenueProjection::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> response = new HashMap<>();
        response.put("totalRevenue", totalRevenue);
        response.put("trend", trend);

        return ApiResponseHelper.successResponse("Revenue trend retrieved successfully", response);
    }

    @GetMapping("/revenue-by-lab")
    public ResponseEntity<?> getRevenueByLab(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        List<BillingRepository.RevenueByLabProjection> revenueByLab;
        if (startDate != null && endDate != null) {
            revenueByLab = billingRepository.getRevenueByLab(currentUser.getId(), toInstantStart(startDate), toInstantEnd(endDate));
        } else {
            revenueByLab = billingRepository.getRevenueByLabAllTime(currentUser.getId());
        }

        return ApiResponseHelper.successResponse("Revenue by lab retrieved successfully", revenueByLab);
    }

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
