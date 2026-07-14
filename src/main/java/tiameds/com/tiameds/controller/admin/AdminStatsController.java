package tiameds.com.tiameds.controller.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.BillingRepository;
import tiameds.com.tiameds.repository.DoctorRepository;
import tiameds.com.tiameds.repository.LabRepository;
import tiameds.com.tiameds.repository.PatientRepository;
import tiameds.com.tiameds.repository.TestRepository;
import tiameds.com.tiameds.repository.UserRepository;
import tiameds.com.tiameds.repository.VisitRepository;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/lab-admin/stats")
@Tag(name = "Admin Stats Controller", description = "Statistics endpoints for lab admin scoped to a specific lab")
public class AdminStatsController {

    private final LabRepository labRepository;
    private final PatientRepository patientRepository;
    private final VisitTestResultRepository visitTestResultRepository;
    private final UserRepository userRepository;
    private final TestRepository testRepository;
    private final BillingRepository billingRepository;
    private final VisitRepository visitRepository;
    private final DoctorRepository doctorRepository;
    private final UserAuthService userAuthService;

    public AdminStatsController(LabRepository labRepository,
                                PatientRepository patientRepository,
                                VisitTestResultRepository visitTestResultRepository,
                                UserRepository userRepository,
                                TestRepository testRepository,
                                BillingRepository billingRepository,
                                VisitRepository visitRepository,
                                DoctorRepository doctorRepository,
                                UserAuthService userAuthService) {
        this.labRepository = labRepository;
        this.patientRepository = patientRepository;
        this.visitTestResultRepository = visitTestResultRepository;
        this.userRepository = userRepository;
        this.testRepository = testRepository;
        this.billingRepository = billingRepository;
        this.visitRepository = visitRepository;
        this.doctorRepository = doctorRepository;
        this.userAuthService = userAuthService;
    }

    // ─── Auth & access helpers ────────────────────────────────────────────────

    private ResponseEntity<?> authenticate(String token, Long labId, Object[] outUser, Object[] outLab) {
        Optional<User> userOpt = userAuthService.authenticateUser(token);
        if (userOpt.isEmpty())
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);

        User user = userOpt.get();
        Optional<Lab> labOpt = labRepository.findById(labId);
        if (labOpt.isEmpty())
            return ApiResponseHelper.errorResponse("Lab not found", HttpStatus.NOT_FOUND);

        if (!userRepository.existsByIdAndLabsId(user.getId(), labId))
            return ApiResponseHelper.errorResponse("You are not authorized to access this lab's stats", HttpStatus.FORBIDDEN);

        outUser[0] = user;
        outLab[0] = labOpt.get();
        return null;
    }

    // ─── Endpoints ────────────────────────────────────────────────────────────

    @GetMapping("/my-labs/count")
    public ResponseEntity<?> getMyLabsCount(@RequestHeader("Authorization") String token) {
        Optional<User> userOpt = userAuthService.authenticateUser(token);
        if (userOpt.isEmpty())
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);

        Set<Lab> labs = labRepository.findLabsByUserId(userOpt.get().getId());
        return ApiResponseHelper.successResponse("Total labs retrieved successfully", Map.of("totalLabs", labs.size()));
    }

    @GetMapping("/{labId}/total-admins")
    public ResponseEntity<?> getTotalAdmins(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        long count = (startDate != null && endDate != null)
                ? userRepository.countByRolesNameAndLabsIdAndCreatedAtBetween("ADMIN", labId, toStart(startDate), toEnd(endDate))
                : userRepository.countByRolesNameAndLabsId("ADMIN", labId);
        return ApiResponseHelper.successResponse("Total admins retrieved successfully", Map.of("totalAdmins", count));
    }

    @GetMapping("/{labId}/total-technicians")
    public ResponseEntity<?> getTotalTechnicians(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        long count = (startDate != null && endDate != null)
                ? userRepository.countByRolesNameAndLabsIdAndCreatedAtBetween("TECHNICIAN", labId, toStart(startDate), toEnd(endDate))
                : userRepository.countByRolesNameAndLabsId("TECHNICIAN", labId);
        return ApiResponseHelper.successResponse("Total technicians retrieved successfully", Map.of("totalTechnicians", count));
    }

    @GetMapping("/{labId}/total-deskroles")
    public ResponseEntity<?> getTotalDeskRoles(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        long count = (startDate != null && endDate != null)
                ? userRepository.countByRolesNameAndLabsIdAndCreatedAtBetween("DESKROLE", labId, toStart(startDate), toEnd(endDate))
                : userRepository.countByRolesNameAndLabsId("DESKROLE", labId);
        return ApiResponseHelper.successResponse("Total desk roles retrieved successfully", Map.of("totalDeskRoles", count));
    }

    @GetMapping("/{labId}/total-tests")
    public ResponseEntity<?> getTotalTests(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        long count = (startDate != null && endDate != null)
                ? visitTestResultRepository.countAllTestsByLabIdAndCreatedAtBetween(labId, toStart(startDate), toEnd(endDate))
                : visitTestResultRepository.countAllTestsByLabId(labId);
        return ApiResponseHelper.successResponse("Total tests retrieved successfully", Map.of("totalTests", count));
    }

    @GetMapping("/{labId}/total-revenue")
    public ResponseEntity<?> getTotalRevenue(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        BigDecimal revenue = (startDate != null && endDate != null)
                ? billingRepository.sumTotalByLabId(labId, toInstantStart(startDate), toInstantEnd(endDate))
                : billingRepository.sumTotalRevenueByLabIdAllTime(labId);
        if (revenue == null) revenue = BigDecimal.ZERO;
        return ApiResponseHelper.successResponse("Total revenue retrieved successfully", Map.of("totalRevenue", revenue));
    }

    @GetMapping("/{labId}/reports-generated")
    public ResponseEntity<?> getReportsGenerated(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        long count = (startDate != null && endDate != null)
                ? visitTestResultRepository.countCompletedReportsByLabIdAndCreatedAtBetween(labId, toStart(startDate), toEnd(endDate))
                : visitTestResultRepository.countCompletedReportsByLabId(labId);
        return ApiResponseHelper.successResponse("Reports generated retrieved successfully", Map.of("reportsGenerated", count));
    }

    @GetMapping("/{labId}/pending-samples")
    public ResponseEntity<?> getPendingSamples(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        long count = (startDate != null && endDate != null)
                ? visitRepository.countPendingVisitsByLabIdAndCreatedAtBetween(labId, toInstantStart(startDate), toInstantEnd(endDate))
                : visitRepository.countPendingVisitsByLabId(labId);
        return ApiResponseHelper.successResponse("Pending samples retrieved successfully", Map.of("pendingSamples", count));
    }

    @GetMapping("/{labId}/tests-by-category")
    public ResponseEntity<?> getTestsByCategory(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        List<TestRepository.TestsByCategoryProjection> categories = (startDate != null && endDate != null)
                ? testRepository.getTestsByCategoryByLabIdWithDateRange(labId, toStart(startDate), toEnd(endDate))
                : testRepository.getTestsByCategoryByLabId(labId);

        long total = categories.stream().mapToLong(TestRepository.TestsByCategoryProjection::getTestCount).sum();

        Map<String, Object> response = new HashMap<>();
        response.put("total", total);
        response.put("categories", categories);
        return ApiResponseHelper.successResponse("Tests by category retrieved successfully", response);
    }

    @GetMapping("/{labId}/revenue-trend")
    public ResponseEntity<?> getRevenueTrend(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        Instant start = toInstantStart(startDate);
        Instant end   = toInstantEnd(endDate);
        List<BillingRepository.DailyRevenueProjection> trend = billingRepository.getDailyRevenueTrendByLabId(labId, start, end);

        BigDecimal totalRevenue = trend.stream()
                .map(BillingRepository.DailyRevenueProjection::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> response = new HashMap<>();
        response.put("totalRevenue", totalRevenue);
        response.put("trend", trend);
        return ApiResponseHelper.successResponse("Revenue trend retrieved successfully", response);
    }

    @GetMapping("/{labId}/revenue-by-lab")
    public ResponseEntity<?> getRevenueByLab(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        Optional<BillingRepository.RevenueByLabProjection> result = (startDate != null && endDate != null)
                ? billingRepository.getRevenueByLabIdAndDateRange(labId, toInstantStart(startDate), toInstantEnd(endDate))
                : billingRepository.getRevenueByLabId(labId);
        return ApiResponseHelper.successResponse("Revenue by lab retrieved successfully", result.orElse(null));
    }

    @GetMapping("/{labId}/patient/{patientId}/test-summary")
    public ResponseEntity<?> getPatientTestSummary(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @PathVariable Long patientId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        try {
            Object[] u = new Object[1], l = new Object[1];
            ResponseEntity<?> err = authenticate(token, labId, u, l);
            if (err != null) return err;

            if (!patientRepository.existsByPatientIdAndLabsId(patientId, labId))
                return ApiResponseHelper.errorResponse("Patient not found for the specified lab", HttpStatus.NOT_FOUND);

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

    @GetMapping("/{labId}/lab-performance")
    public ResponseEntity<?> getLabPerformance(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        LabRepository.LabPerformanceSummaryProjection row;
        Double growthPct = null;

        if (startDate != null && endDate != null) {
            Instant start = toInstantStart(startDate);
            Instant end   = toInstantEnd(endDate);
            long periodDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            LocalDate prevEnd   = startDate.minusDays(1);
            LocalDate prevStart = prevEnd.minusDays(periodDays - 1);

            Optional<LabRepository.LabPerformanceSummaryProjection> opt =
                    labRepository.getLabPerformanceByLabIdAndDateRange(
                            labId, start, end, toInstantStart(prevStart), toInstantEnd(prevEnd));
            if (opt.isEmpty())
                return ApiResponseHelper.errorResponse("No performance data found for this lab", HttpStatus.NOT_FOUND);
            row = opt.get();

            BigDecimal revenue = row.getRevenue()         != null ? row.getRevenue()         : BigDecimal.ZERO;
            BigDecimal prevRev = row.getPreviousRevenue() != null ? row.getPreviousRevenue() : BigDecimal.ZERO;
            if (prevRev.compareTo(BigDecimal.ZERO) > 0) {
                growthPct = revenue.subtract(prevRev)
                        .divide(prevRev, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue();
            } else {
                growthPct = revenue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
            }
        } else {
            Optional<LabRepository.LabPerformanceSummaryProjection> opt =
                    labRepository.getLabPerformanceByLabIdAllTime(labId);
            if (opt.isEmpty())
                return ApiResponseHelper.errorResponse("No performance data found for this lab", HttpStatus.NOT_FOUND);
            row = opt.get();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labId",            row.getLabId());
        result.put("labName",          row.getLabName());
        result.put("revenue",          row.getRevenue()         != null ? row.getRevenue()         : BigDecimal.ZERO);
        result.put("tests",            row.getTestCount());
        result.put("patients",         row.getPatientCount());
        result.put("pendingSamples",   row.getPendingSamples());
        result.put("avgTatHours",      row.getAvgTatHours());
        result.put("reportsGenerated", row.getReportsGenerated());
        result.put("growthPct",        growthPct);
        return ApiResponseHelper.successResponse("Lab performance retrieved successfully", result);
    }

    @GetMapping("/{labId}/total-patients")
    public ResponseEntity<?> getTotalPatients(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        long count = (startDate != null && endDate != null)
                ? patientRepository.countByLabIdAndCreatedAtBetween(labId, toInstantStart(startDate), toInstantEnd(endDate))
                : patientRepository.countByLabId(labId);
        return ApiResponseHelper.successResponse("Total patients retrieved successfully", Map.of("totalPatients", count));
    }

    @GetMapping("/{labId}/top-referring-doctors")
    public ResponseEntity<?> getTopReferringDoctors(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        List<DoctorRepository.TopReferringDoctorProjection> doctors = (startDate != null && endDate != null)
                ? doctorRepository.getTopReferringDoctorsByLabIdWithDateRange(labId, toInstantStart(startDate), toInstantEnd(endDate), limit)
                : doctorRepository.getTopReferringDoctorsByLabId(labId, limit);
        return ApiResponseHelper.successResponse("Top referring doctors retrieved successfully", doctors);
    }

    @GetMapping("/{labId}/avg-tat")
    public ResponseEntity<?> getAvgTat(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        Double avg = (startDate != null && endDate != null)
                ? visitTestResultRepository.getAvgTatHoursByLabIdAndDateRange(labId, toInstantStart(startDate), toInstantEnd(endDate))
                : visitTestResultRepository.getAvgTatHoursByLabId(labId);
        double avgTat = (avg != null) ? Math.round(avg * 10.0) / 10.0 : 0.0;
        return ApiResponseHelper.successResponse("Avg TAT retrieved successfully", Map.of("avgTatHours", avgTat));
    }

    @GetMapping("/{labId}/dashboard-kpis")
    public ResponseEntity<?> getDashboardKpis(
            @RequestHeader("Authorization") String token,
            @PathVariable Long labId) {
        Object[] u = new Object[1], l = new Object[1];
        ResponseEntity<?> err = authenticate(token, labId, u, l);
        if (err != null) return err;

        LocalDate today       = LocalDate.now();
        LocalDate currStart   = today.minusDays(6);
        LocalDate prevStart   = today.minusDays(13);
        LocalDate prevEnd     = today.minusDays(7);

        Instant iCurrStart = toInstantStart(currStart);
        Instant iCurrEnd   = toInstantEnd(today);
        Instant iPrevStart = toInstantStart(prevStart);
        Instant iPrevEnd   = toInstantEnd(prevEnd);
        LocalDateTime lCurrStart = toStart(currStart);
        LocalDateTime lCurrEnd   = toEnd(today);
        LocalDateTime lPrevStart = toStart(prevStart);
        LocalDateTime lPrevEnd   = toEnd(prevEnd);

        // ── Revenue ──
        BigDecimal currRevenue = billingRepository.sumTotalByLabId(labId, iCurrStart, iCurrEnd);
        BigDecimal prevRevenue = billingRepository.sumTotalByLabId(labId, iPrevStart, iPrevEnd);
        if (currRevenue == null) currRevenue = BigDecimal.ZERO;
        if (prevRevenue == null) prevRevenue = BigDecimal.ZERO;

        // ── Tests ──
        long currTests = visitTestResultRepository.countAllTestsByLabIdAndCreatedAtBetween(labId, lCurrStart, lCurrEnd);
        long prevTests = visitTestResultRepository.countAllTestsByLabIdAndCreatedAtBetween(labId, lPrevStart, lPrevEnd);

        // ── Patients ──
        long currPatients = patientRepository.countByLabIdAndCreatedAtBetween(labId, iCurrStart, iCurrEnd);
        long prevPatients = patientRepository.countByLabIdAndCreatedAtBetween(labId, iPrevStart, iPrevEnd);

        // ── Pending Samples ──
        long currPending = visitRepository.countPendingVisitsByLabIdAndCreatedAtBetween(labId, iCurrStart, iCurrEnd);
        long prevPending = visitRepository.countPendingVisitsByLabIdAndCreatedAtBetween(labId, iPrevStart, iPrevEnd);

        // ── Reports Generated ──
        long currReports = visitTestResultRepository.countCompletedReportsByLabIdAndCreatedAtBetween(labId, lCurrStart, lCurrEnd);
        long prevReports = visitTestResultRepository.countCompletedReportsByLabIdAndCreatedAtBetween(labId, lPrevStart, lPrevEnd);

        // ── Avg TAT ──
        Double currTatRaw = visitTestResultRepository.getAvgTatHoursByLabIdAndDateRange(labId, iCurrStart, iCurrEnd);
        Double prevTatRaw = visitTestResultRepository.getAvgTatHoursByLabIdAndDateRange(labId, iPrevStart, iPrevEnd);
        double currTat = currTatRaw != null ? Math.round(currTatRaw * 10.0) / 10.0 : 0.0;
        double prevTat = prevTatRaw != null ? Math.round(prevTatRaw * 10.0) / 10.0 : 0.0;

        // ── Role counts (all-time, no comparison needed) ──
        long admins      = userRepository.countByRolesNameAndLabsId("ADMIN", labId);
        long deskUsers   = userRepository.countByRolesNameAndLabsId("DESKROLE", labId);
        long technicians = userRepository.countByRolesNameAndLabsId("TECHNICIAN", labId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalRevenue",      kpiMap(currRevenue, pctChange(currRevenue, prevRevenue)));
        response.put("totalTests",        kpiMap(currTests,   pctChange(currTests, prevTests)));
        response.put("totalPatients",     kpiMap(currPatients,pctChange(currPatients, prevPatients)));
        response.put("pendingSamples",    kpiMap(currPending, pctChange(currPending, prevPending)));
        response.put("reportsGenerated",  kpiMap(currReports, pctChange(currReports, prevReports)));
        response.put("avgTatHours",       tatKpiMap(currTat, currTat - prevTat));
        response.put("activeAdmins",      Map.of("value", admins));
        response.put("deskUsers",         Map.of("value", deskUsers));
        response.put("technicians",       Map.of("value", technicians));
        return ApiResponseHelper.successResponse("Dashboard KPIs retrieved successfully", response);
    }

    private Map<String, Object> kpiMap(Object value, double vsLastWeekPct) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("value", value);
        m.put("vsLastWeekPct", vsLastWeekPct);
        m.put("direction", vsLastWeekPct >= 0 ? "up" : "down");
        return m;
    }

    private Map<String, Object> tatKpiMap(double value, double diffHours) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("value", value);
        m.put("vsLastWeekHours", Math.round(diffHours * 10.0) / 10.0);
        m.put("direction", diffHours <= 0 ? "down" : "up");
        return m;
    }

    private double pctChange(long curr, long prev) {
        if (prev == 0) return curr > 0 ? 100.0 : 0.0;
        return Math.round(((double)(curr - prev) / prev) * 1000.0) / 10.0;
    }

    private double pctChange(BigDecimal curr, BigDecimal prev) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0)
            return curr != null && curr.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        if (curr == null) curr = BigDecimal.ZERO;
        return curr.subtract(prev)
                .divide(prev, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // ─── Date helpers ─────────────────────────────────────────────────────────

    private LocalDateTime toStart(LocalDate date) { return date.atStartOfDay(); }
    private LocalDateTime toEnd(LocalDate date)   { return date.atTime(LocalTime.MAX); }
    private Instant toInstantStart(LocalDate date) { return date.atStartOfDay(ZoneId.systemDefault()).toInstant(); }
    private Instant toInstantEnd(LocalDate date)   { return date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant(); }
}
