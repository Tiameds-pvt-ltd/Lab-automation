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

    @GetMapping("/tests-by-category")
    public ResponseEntity<?> getTestsByCategory(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        List<VisitTestResultRepository.TestsByCategoryDetailedProjection> categories;
        if (startDate != null && endDate != null) {
            categories = visitTestResultRepository.getPatientTestsByCategoryDetailedBySuperAdminWithDateRange(currentUser.getId(), toStart(startDate), toEnd(endDate));
        } else {
            categories = visitTestResultRepository.getPatientTestsByCategoryDetailedBySuperAdmin(currentUser.getId());
        }

        long totalTests = categories.stream()
                .mapToLong(VisitTestResultRepository.TestsByCategoryDetailedProjection::getTestCount)
                .sum();
        BigDecimal totalRevenue    = categories.stream().map(VisitTestResultRepository.TestsByCategoryDetailedProjection::getRevenue).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalDiscount   = categories.stream().map(VisitTestResultRepository.TestsByCategoryDetailedProjection::getDiscount).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalPaid       = categories.stream().map(VisitTestResultRepository.TestsByCategoryDetailedProjection::getPaidRevenue).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalDue        = categories.stream().map(VisitTestResultRepository.TestsByCategoryDetailedProjection::getDueRevenue).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalCash       = categories.stream().map(VisitTestResultRepository.TestsByCategoryDetailedProjection::getCashRevenue).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalUpi        = categories.stream().map(VisitTestResultRepository.TestsByCategoryDetailedProjection::getUpiRevenue).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalCard       = categories.stream().map(VisitTestResultRepository.TestsByCategoryDetailedProjection::getCardRevenue).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTests",    totalTests);
        summary.put("totalRevenue",  totalRevenue);
        summary.put("totalDiscount", totalDiscount);
        summary.put("totalPaid",     totalPaid);
        summary.put("totalDue",      totalDue);
        summary.put("totalCash",     totalCash);
        summary.put("totalUpi",      totalUpi);
        summary.put("totalCard",     totalCard);

        Map<String, Object> response = new HashMap<>();
        response.put("summary", summary);
        response.put("categories", categories);

        return ApiResponseHelper.successResponse("Tests by category retrieved successfully", response);
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

    @GetMapping("/lab-performance")
    public ResponseEntity<?> getLabPerformanceSummary(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        List<LabRepository.LabPerformanceSummaryProjection> results;

        if (startDate != null && endDate != null) {
            Instant start = toInstantStart(startDate);
            Instant end   = toInstantEnd(endDate);

            long periodDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            LocalDate prevEnd   = startDate.minusDays(1);
            LocalDate prevStart = prevEnd.minusDays(periodDays - 1);

            results = labRepository.getLabPerformanceSummary(
                    currentUser.getId(), start, end,
                    toInstantStart(prevStart), toInstantEnd(prevEnd), limit);
        } else {
            results = labRepository.getLabPerformanceSummaryAllTime(currentUser.getId(), limit);
        }

        List<Map<String, Object>> labs = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            LabRepository.LabPerformanceSummaryProjection row = results.get(i);
            BigDecimal revenue  = row.getRevenue()         != null ? row.getRevenue()         : BigDecimal.ZERO;
            BigDecimal prevRev  = row.getPreviousRevenue() != null ? row.getPreviousRevenue() : BigDecimal.ZERO;

            Double growthPct = null;
            if (startDate != null && endDate != null) {
                if (prevRev.compareTo(BigDecimal.ZERO) > 0) {
                    growthPct = revenue.subtract(prevRev)
                            .divide(prevRev, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP)
                            .doubleValue();
                } else {
                    growthPct = revenue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
                }
            }

            Map<String, Object> lab = new LinkedHashMap<>();
            lab.put("rank", i + 1);
            lab.put("labId", row.getLabId());
            lab.put("labName", row.getLabName());
            lab.put("revenue", revenue);
            lab.put("tests", row.getTestCount());
            lab.put("patients", row.getPatientCount());
            lab.put("pendingSamples", row.getPendingSamples());
            lab.put("avgTatHours", row.getAvgTatHours());
            lab.put("reportsGenerated", row.getReportsGenerated());
            lab.put("growthPct", growthPct);
            labs.add(lab);
        }

        return ApiResponseHelper.successResponse("Lab performance summary retrieved successfully", labs);
    }

    @GetMapping("/top-referring-doctors")
    public ResponseEntity<?> getTopReferringDoctors(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        List<DoctorRepository.TopReferringDoctorProjection> doctors;
        if (startDate != null && endDate != null) {
            doctors = doctorRepository.getTopReferringDoctorsWithDateRange(
                    currentUser.getId(), toInstantStart(startDate), toInstantEnd(endDate), limit);
        } else {
            doctors = doctorRepository.getTopReferringDoctors(currentUser.getId(), limit);
        }

        return ApiResponseHelper.successResponse("Top referring doctors retrieved successfully", doctors);
    }

    @GetMapping("/detailed-billing")
    public ResponseEntity<?> getDetailedBilling(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        Long userId = currentUser.getId();

        List<BillingRepository.DetailedBillingSummaryProjection> summaryList;
        List<BillingRepository.BillingByStatusProjection> byStatus;
        List<VisitTestResultRepository.TestsByCategoryDetailedProjection> testCategories;
        List<HealthPackageRepository.PackageSummaryProjection> packages;

        if (startDate != null && endDate != null) {
            Instant iStart = toInstantStart(startDate);
            Instant iEnd   = toInstantEnd(endDate);
            summaryList    = billingRepository.getDetailedBillingSummaryWithDateRange(userId, iStart, iEnd);
            byStatus       = billingRepository.getBillingByStatusWithDateRange(userId, iStart, iEnd);
            testCategories = visitTestResultRepository.getPatientTestsByCategoryDetailedBySuperAdminWithDateRange(userId, toStart(startDate), toEnd(endDate));
            packages       = healthPackageRepository.getPackageSummaryBySuperAdminWithDateRange(userId, iStart, iEnd);
        } else {
            summaryList    = billingRepository.getDetailedBillingSummary(userId);
            byStatus       = billingRepository.getBillingByStatus(userId);
            testCategories = visitTestResultRepository.getPatientTestsByCategoryDetailedBySuperAdmin(userId);
            packages       = healthPackageRepository.getPackageSummaryBySuperAdmin(userId);
        }

        BillingRepository.DetailedBillingSummaryProjection raw = summaryList.isEmpty() ? null : summaryList.get(0);

        // Top-level billing summary
        // grossBilled  = total_amount (sum of test + package prices before discount)
        // totalDiscount = discount applied
        // netBilled    = net_amount = grossBilled - discount (what patient actually owes)
        // totalPaid    = actual_received_amount (cumulative, updated per transaction)
        // totalDue     = due_amount (decreases as more payments arrive)
        Map<String, Object> paymentMode = new LinkedHashMap<>();
        paymentMode.put("cash", safe(raw != null ? raw.getTotalCash() : null));
        paymentMode.put("upi",  safe(raw != null ? raw.getTotalUpi()  : null));
        paymentMode.put("card", safe(raw != null ? raw.getTotalCard() : null));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalBillings", raw != null ? raw.getTotalBillings() : 0L);
        summary.put("grossBilled",   safe(raw != null ? raw.getGrossRevenue()  : null));
        summary.put("totalDiscount", safe(raw != null ? raw.getTotalDiscount() : null));
        summary.put("totalGst",      safe(raw != null ? raw.getTotalGst()      : null));
        summary.put("netBilled",     safe(raw != null ? raw.getNetRevenue()    : null));
        summary.put("totalPaid",     safe(raw != null ? raw.getTotalPaid()     : null));
        summary.put("totalDue",      safe(raw != null ? raw.getTotalDue()      : null));
        summary.put("paymentMode",   paymentMode);

        // Breakdown by payment status (PAID / PARTIAL / PENDING)
        List<Map<String, Object>> statusBreakdown = new ArrayList<>();
        for (BillingRepository.BillingByStatusProjection s : byStatus) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("status",      s.getStatus());
            row.put("count",       s.getBillingCount());
            row.put("grossBilled", safe(s.getGrossRevenue()));
            row.put("discount",    safe(s.getTotalDiscount()));
            row.put("gst",         safe(s.getTotalGst()));
            row.put("netBilled",   safe(s.getNetRevenue()));
            row.put("paid",        safe(s.getTotalPaid()));
            row.put("due",         safe(s.getTotalDue()));
            statusBreakdown.add(row);
        }

        // Test-category billing breakdown
        long       tcTests    = testCategories.stream().mapToLong(VisitTestResultRepository.TestsByCategoryDetailedProjection::getTestCount).sum();
        BigDecimal tcRevenue  = sumField(testCategories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getRevenue);
        BigDecimal tcDiscount = sumField(testCategories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getDiscount);
        BigDecimal tcPaid     = sumField(testCategories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getPaidRevenue);
        BigDecimal tcDue      = sumField(testCategories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getDueRevenue);
        BigDecimal tcCash     = sumField(testCategories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getCashRevenue);
        BigDecimal tcUpi      = sumField(testCategories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getUpiRevenue);
        BigDecimal tcCard     = sumField(testCategories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getCardRevenue);

        Map<String, Object> testsSummary = new LinkedHashMap<>();
        testsSummary.put("totalCategories", testCategories.size());
        testsSummary.put("totalTests",  tcTests);
        testsSummary.put("grossBilled", tcRevenue);
        testsSummary.put("discount",    tcDiscount);
        testsSummary.put("paid",        tcPaid);
        testsSummary.put("due",         tcDue);
        testsSummary.put("paymentMode", Map.of("cash", tcCash, "upi", tcUpi, "card", tcCard));

        // Package billing breakdown
        BigDecimal pkgRevenue  = sumField(packages, HealthPackageRepository.PackageSummaryProjection::getRevenue);
        BigDecimal pkgDiscount = sumField(packages, HealthPackageRepository.PackageSummaryProjection::getDiscount);
        BigDecimal pkgPaid     = sumField(packages, HealthPackageRepository.PackageSummaryProjection::getPaidRevenue);
        BigDecimal pkgDue      = sumField(packages, HealthPackageRepository.PackageSummaryProjection::getDueRevenue);
        BigDecimal pkgCash     = sumField(packages, HealthPackageRepository.PackageSummaryProjection::getCashRevenue);
        BigDecimal pkgUpi      = sumField(packages, HealthPackageRepository.PackageSummaryProjection::getUpiRevenue);
        BigDecimal pkgCard     = sumField(packages, HealthPackageRepository.PackageSummaryProjection::getCardRevenue);

        Map<String, Object> packageSummary = new LinkedHashMap<>();
        packageSummary.put("totalPackages", packages.size());
        packageSummary.put("totalVisits",   packages.stream().mapToLong(p -> p.getVisitCount() != null ? p.getVisitCount() : 0L).sum());
        packageSummary.put("grossBilled",   pkgRevenue);
        packageSummary.put("discount",      pkgDiscount);
        packageSummary.put("paid",          pkgPaid);
        packageSummary.put("due",           pkgDue);
        packageSummary.put("paymentMode",   Map.of("cash", pkgCash, "upi", pkgUpi, "card", pkgCard));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary",          summary);
        response.put("byPaymentStatus",  statusBreakdown);
        response.put("testsSummary",     testsSummary);
        response.put("testCategories",   testCategories);
        response.put("packageSummary",   packageSummary);
        response.put("packages",         packages);

        return ApiResponseHelper.successResponse("Detailed billing retrieved successfully", response);
    }

    @GetMapping("/packages-summary")
    public ResponseEntity<?> getPackagesSummary(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        List<HealthPackageRepository.PackageSummaryProjection> packages;
        if (startDate != null && endDate != null) {
            packages = healthPackageRepository.getPackageSummaryBySuperAdminWithDateRange(
                    currentUser.getId(), toInstantStart(startDate), toInstantEnd(endDate));
        } else {
            packages = healthPackageRepository.getPackageSummaryBySuperAdmin(currentUser.getId());
        }

        long totalVisits     = packages.stream().mapToLong(p -> p.getVisitCount() != null ? p.getVisitCount() : 0L).sum();
        BigDecimal totalRevenue  = packages.stream().map(HealthPackageRepository.PackageSummaryProjection::getRevenue).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalDiscount = packages.stream().map(HealthPackageRepository.PackageSummaryProjection::getDiscount).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalPaid     = packages.stream().map(HealthPackageRepository.PackageSummaryProjection::getPaidRevenue).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalDue      = packages.stream().map(HealthPackageRepository.PackageSummaryProjection::getDueRevenue).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalCash     = packages.stream().map(HealthPackageRepository.PackageSummaryProjection::getCashRevenue).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalUpi      = packages.stream().map(HealthPackageRepository.PackageSummaryProjection::getUpiRevenue).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalCard     = packages.stream().map(HealthPackageRepository.PackageSummaryProjection::getCardRevenue).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalPackages",  packages.size());
        summary.put("totalVisits",    totalVisits);
        summary.put("totalRevenue",   totalRevenue);
        summary.put("totalDiscount",  totalDiscount);
        summary.put("totalPaid",      totalPaid);
        summary.put("totalDue",       totalDue);
        summary.put("totalCash",      totalCash);
        summary.put("totalUpi",       totalUpi);
        summary.put("totalCard",      totalCard);

        Map<String, Object> response = new HashMap<>();
        response.put("summary",  summary);
        response.put("packages", packages);

        return ApiResponseHelper.successResponse("Packages summary retrieved successfully", response);
    }

    @GetMapping("/dashboard-summary")
    public ResponseEntity<?> getDashboardSummary(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        Long userId = currentUser.getId();

        List<LabRepository.LabPerformanceSummaryProjection> labRows;
        if (startDate != null && endDate != null) {
            long periodDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            LocalDate prevEnd   = startDate.minusDays(1);
            LocalDate prevStart = prevEnd.minusDays(periodDays - 1);
            labRows = labRepository.getAllLabsSummaryWithDateRange(
                    userId,
                    toInstantStart(startDate), toInstantEnd(endDate),
                    toInstantStart(prevStart), toInstantEnd(prevEnd));
        } else {
            labRows = labRepository.getAllLabsSummaryAllTime(userId);
        }

        long totalLabs             = labRows.size();
        BigDecimal totalRevenue    = BigDecimal.ZERO;
        long totalTests            = 0;
        long totalPatients         = 0;
        long totalPendingSamples   = 0;
        long totalReportsGenerated = 0;

        List<Map<String, Object>> labWise = new ArrayList<>();
        for (LabRepository.LabPerformanceSummaryProjection row : labRows) {
            BigDecimal rev = row.getRevenue()          != null ? row.getRevenue()          : BigDecimal.ZERO;
            long tests     = row.getTestCount()        != null ? row.getTestCount()        : 0L;
            long patients  = row.getPatientCount()     != null ? row.getPatientCount()     : 0L;
            long pending   = row.getPendingSamples()   != null ? row.getPendingSamples()   : 0L;
            long reports   = row.getReportsGenerated() != null ? row.getReportsGenerated() : 0L;

            totalRevenue          = totalRevenue.add(rev);
            totalTests           += tests;
            totalPatients        += patients;
            totalPendingSamples  += pending;
            totalReportsGenerated+= reports;

            Map<String, Object> lab = new LinkedHashMap<>();
            lab.put("labId",            row.getLabId());
            lab.put("labName",          row.getLabName());
            lab.put("revenue",          rev.setScale(2, RoundingMode.HALF_UP));
            lab.put("tests",            tests);
            lab.put("patients",         patients);
            lab.put("pendingSamples",   pending);
            lab.put("reportsGenerated", reports);
            lab.put("avgTatHours",      row.getAvgTatHours());
            labWise.add(lab);
        }

        Map<String, Object> cumulative = new LinkedHashMap<>();
        cumulative.put("totalLabs",        totalLabs);
        cumulative.put("totalRevenue",     totalRevenue.setScale(2, RoundingMode.HALF_UP));
        cumulative.put("totalTests",       totalTests);
        cumulative.put("totalPatients",    totalPatients);
        cumulative.put("reportsGenerated", totalReportsGenerated);
        cumulative.put("pendingSamples",   totalPendingSamples);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("cumulative", cumulative);
        response.put("labWise",    labWise);

        return ApiResponseHelper.successResponse("Dashboard summary retrieved successfully", response);
    }

    @GetMapping("/earnings-by-category")
    public ResponseEntity<?> getEarningsByCategory(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }


        User currentUser = userOptional.get();
        List<VisitTestResultRepository.TestEarningsByTestProjection> rows;
        if (startDate != null && endDate != null) {
            rows = visitTestResultRepository.getEarningsByTestBySuperAdminWithDateRange(
                    currentUser.getId(), toStart(startDate), toEnd(endDate));
        } else {
            rows = visitTestResultRepository.getEarningsByTestBySuperAdmin(currentUser.getId());
        }

        // Group rows by category
        Map<String, List<VisitTestResultRepository.TestEarningsByTestProjection>> byCategory = new LinkedHashMap<>();
        for (VisitTestResultRepository.TestEarningsByTestProjection row : rows) {
            byCategory.computeIfAbsent(row.getCategory(), k -> new ArrayList<>()).add(row);
        }

        BigDecimal grandTotalEarnings = BigDecimal.ZERO;
        BigDecimal grandTotalPaid     = BigDecimal.ZERO;
        BigDecimal grandTotalDue      = BigDecimal.ZERO;
        long       grandTotalTests    = 0;

        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map.Entry<String, List<VisitTestResultRepository.TestEarningsByTestProjection>> entry : byCategory.entrySet()) {
            List<VisitTestResultRepository.TestEarningsByTestProjection> testRows = entry.getValue();

            BigDecimal catEarnings = BigDecimal.ZERO;
            BigDecimal catPaid     = BigDecimal.ZERO;
            BigDecimal catDue      = BigDecimal.ZERO;
            long       catCount    = 0;

            List<Map<String, Object>> tests = new ArrayList<>();
            for (VisitTestResultRepository.TestEarningsByTestProjection t : testRows) {
                BigDecimal te = safe(t.getTotalEarnings());
                BigDecimal tp = safe(t.getPaidAmount());
                BigDecimal td = safe(t.getDueAmount());
                long       tc = t.getOrderedCount() != null ? t.getOrderedCount() : 0L;

                catEarnings = catEarnings.add(te);
                catPaid     = catPaid.add(tp);
                catDue      = catDue.add(td);
                catCount   += tc;

                Map<String, Object> testMap = new LinkedHashMap<>();
                testMap.put("testId",        t.getTestId());
                testMap.put("testName",      t.getTestName());
                testMap.put("testCode",      t.getTestCode());
                testMap.put("price",         safe(t.getTestPrice()));
                testMap.put("orderedCount",  tc);
                testMap.put("totalEarnings", te);
                testMap.put("paidAmount",    tp);
                testMap.put("dueAmount",     td);
                tests.add(testMap);
            }

            grandTotalEarnings = grandTotalEarnings.add(catEarnings);
            grandTotalPaid     = grandTotalPaid.add(catPaid);
            grandTotalDue      = grandTotalDue.add(catDue);
            grandTotalTests   += catCount;

            Map<String, Object> catMap = new LinkedHashMap<>();
            catMap.put("category",      entry.getKey());
            catMap.put("totalTests",    catCount);
            catMap.put("totalEarnings", catEarnings.setScale(2, RoundingMode.HALF_UP));
            catMap.put("paidAmount",    catPaid.setScale(2, RoundingMode.HALF_UP));
            catMap.put("dueAmount",     catDue.setScale(2, RoundingMode.HALF_UP));
            catMap.put("tests",         tests);
            categories.add(catMap);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCategories", categories.size());
        summary.put("totalTests",      grandTotalTests);
        summary.put("totalEarnings",   grandTotalEarnings.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalPaid",       grandTotalPaid.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalDue",        grandTotalDue.setScale(2, RoundingMode.HALF_UP));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("summary",    summary);
        response.put("categories", categories);

        return ApiResponseHelper.successResponse("Earnings by category retrieved successfully", response);
    }

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
