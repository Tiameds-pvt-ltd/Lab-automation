package tiameds.com.tiameds.controller.superAdmin;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.repository.*;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/lab-super-admin/stats")
@Tag(name = "Super Admin Dashboard Controller", description = "Single combined stats endpoint for super admin")
public class SuperAdminDashboardController {

    private final LabRepository labRepository;
    private final UserRepository userRepository;
    private final VisitTestResultRepository visitTestResultRepository;
    private final BillingRepository billingRepository;
    private final VisitRepository visitRepository;
    private final DoctorRepository doctorRepository;
    private final HealthPackageRepository healthPackageRepository;
    private final UserAuthService userAuthService;

    public SuperAdminDashboardController(LabRepository labRepository,
                                         UserRepository userRepository,
                                         VisitTestResultRepository visitTestResultRepository,
                                         BillingRepository billingRepository,
                                         VisitRepository visitRepository,
                                         DoctorRepository doctorRepository,
                                         HealthPackageRepository healthPackageRepository,
                                         UserAuthService userAuthService) {
        this.labRepository = labRepository;
        this.userRepository = userRepository;
        this.visitTestResultRepository = visitTestResultRepository;
        this.billingRepository = billingRepository;
        this.visitRepository = visitRepository;
        this.doctorRepository = doctorRepository;
        this.healthPackageRepository = healthPackageRepository;
        this.userAuthService = userAuthService;
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllStats(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Long labId) {

        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }

        User currentUser = userOptional.get();
        Long userId = currentUser.getId();
        boolean hasDates = startDate != null && endDate != null;

        Map<String, Object> response = new LinkedHashMap<>();

        // ── KPIs ─────────────────────────────────────────────────────────────
        response.put("kpis", buildKpis(currentUser, userId, startDate, endDate, hasDates, labId));

        // ── Dashboard summary (lab-wise rollup) ───────────────────────────────
        response.put("dashboardSummary", buildDashboardSummary(userId, startDate, endDate, hasDates, labId));

        // ── Tests by category ─────────────────────────────────────────────────
        response.put("testsByCategory", buildTestsByCategory(currentUser, userId, startDate, endDate, hasDates, labId));

        // ── Revenue trend (only when date range supplied) ─────────────────────
        if (hasDates) {
            response.put("revenueTrend", buildRevenueTrend(userId, startDate, endDate, labId));
        }

        // ── Revenue by lab ────────────────────────────────────────────────────
        response.put("revenueByLab", buildRevenueByLab(userId, startDate, endDate, hasDates, labId));

        // ── Lab performance ───────────────────────────────────────────────────
        response.put("labPerformance", buildLabPerformance(userId, startDate, endDate, hasDates, limit, labId));

        // ── Top referring doctors ─────────────────────────────────────────────
        response.put("topReferringDoctors", buildTopReferringDoctors(userId, startDate, endDate, hasDates, limit, labId));

        // ── Detailed billing ──────────────────────────────────────────────────
        response.put("detailedBilling", buildDetailedBilling(userId, startDate, endDate, hasDates, labId));

        // ── Packages summary ──────────────────────────────────────────────────
        response.put("packagesSummary", buildPackagesSummary(userId, startDate, endDate, hasDates, labId));

        // ── Earnings by category ──────────────────────────────────────────────
        response.put("earningsByCategory", buildEarningsByCategory(currentUser, startDate, endDate, hasDates, labId));

        return ApiResponseHelper.successResponse("All stats retrieved successfully", response);
    }

    // ─── section builders ────────────────────────────────────────────────────

    private Map<String, Object> buildKpis(User currentUser, Long userId,
                                          LocalDate startDate, LocalDate endDate, boolean hasDates, Long labId) {
        long totalLabs, totalAdmins, totalTechnicians, totalDeskRoles, totalTests, reportsGenerated, pendingSamples;
        BigDecimal totalRevenue;

        if (labId != null) {
            if (hasDates) {
                LocalDateTime s = toStart(startDate);
                LocalDateTime e = toEnd(endDate);
                Instant is = toInstantStart(startDate);
                Instant ie = toInstantEnd(endDate);
                totalAdmins      = userRepository.countByRolesNameAndLabsIdAndCreatedAtBetween("ADMIN", labId, s, e);
                totalTechnicians = userRepository.countByRolesNameAndLabsIdAndCreatedAtBetween("TECHNICIAN", labId, s, e);
                totalDeskRoles   = userRepository.countByRolesNameAndLabsIdAndCreatedAtBetween("DESKROLE", labId, s, e);
                totalTests       = visitTestResultRepository.countAllTestsByLabIdAndCreatedAtBetween(labId, s, e);
                reportsGenerated = visitTestResultRepository.countCompletedReportsByLabIdAndCreatedAtBetween(labId, s, e);
                pendingSamples   = visitRepository.countPendingVisitsByLabIdAndCreatedAtBetween(labId, is, ie);
                totalRevenue     = billingRepository.sumPaidAmountByLabId(labId, is, ie);
            } else {
                totalAdmins      = userRepository.countByRolesNameAndLabsId("ADMIN", labId);
                totalTechnicians = userRepository.countByRolesNameAndLabsId("TECHNICIAN", labId);
                totalDeskRoles   = userRepository.countByRolesNameAndLabsId("DESKROLE", labId);
                totalTests       = visitTestResultRepository.countAllTestsByLabId(labId);
                reportsGenerated = visitTestResultRepository.countCompletedReportsByLabId(labId);
                pendingSamples   = visitRepository.countPendingVisitsByLabId(labId);
                totalRevenue     = billingRepository.sumPaidAmountByLabIdAllTime(labId);
            }
            totalLabs = 1;
        } else if (hasDates) {
            LocalDateTime s = toStart(startDate);
            LocalDateTime e = toEnd(endDate);
            Instant is = toInstantStart(startDate);
            Instant ie = toInstantEnd(endDate);

            totalLabs         = labRepository.countByCreatedByAndCreatedAtBetween(currentUser, s, e);
            totalAdmins       = userRepository.countByRolesNameAndCreatedByAndCreatedAtBetween("ADMIN", currentUser, s, e);
            totalTechnicians  = userRepository.countByRolesNameAndCreatedByAndCreatedAtBetween("TECHNICIAN", currentUser, s, e);
            totalDeskRoles    = userRepository.countByRolesNameAndCreatedByAndCreatedAtBetween("DESKROLE", currentUser, s, e);
            totalTests        = visitTestResultRepository.countAllTestsByLabsCreatedByAndCreatedAtBetween(currentUser, s, e);
            reportsGenerated  = visitTestResultRepository.countCompletedReportsByLabsCreatedByAndCreatedAtBetween(currentUser, s, e);
            pendingSamples    = visitRepository.countPendingVisitsByLabsCreatedByAndCreatedAtBetween(currentUser, is, ie);
            totalRevenue      = billingRepository.sumPaidAmountByLabsCreatedByAndCreatedAtBetween(currentUser, is, ie);
        } else {
            totalLabs         = labRepository.countByCreatedBy(currentUser);
            totalAdmins       = userRepository.countByRolesNameAndCreatedBy("ADMIN", currentUser);
            totalTechnicians  = userRepository.countByRolesNameAndCreatedBy("TECHNICIAN", currentUser);
            totalDeskRoles    = userRepository.countByRolesNameAndCreatedBy("DESKROLE", currentUser);
            totalTests        = visitTestResultRepository.countAllTestsByLabsCreatedBy(currentUser);
            reportsGenerated  = visitTestResultRepository.countCompletedReportsByLabsCreatedBy(currentUser);
            pendingSamples    = visitRepository.countPendingVisitsByLabsCreatedBy(currentUser);
            totalRevenue      = billingRepository.sumPaidAmountByLabsCreatedBy(currentUser);
        }

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalLabs",        totalLabs);
        kpis.put("totalAdmins",      totalAdmins);
        kpis.put("totalTechnicians", totalTechnicians);
        kpis.put("totalDeskRoles",   totalDeskRoles);
        kpis.put("totalTests",       totalTests);
        kpis.put("totalRevenue",     safe(totalRevenue));
        kpis.put("reportsGenerated", reportsGenerated);
        kpis.put("pendingSamples",   pendingSamples);
        return kpis;
    }

    private Map<String, Object> buildDashboardSummary(Long userId,
                                                       LocalDate startDate, LocalDate endDate, boolean hasDates, Long labId) {
        List<LabRepository.LabPerformanceSummaryProjection> labRows;
        if (hasDates) {
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

        if (labId != null) {
            final Long filterLabId = labId;
            labRows = labRows.stream()
                    .filter(r -> r.getLabId() != null && r.getLabId().equals(filterLabId))
                    .collect(Collectors.toList());
        }

        BigDecimal totalRevenue    = BigDecimal.ZERO;
        long totalTests            = 0, totalPatients = 0, totalPending = 0, totalReports = 0;

        List<Map<String, Object>> labWise = new ArrayList<>();
        for (LabRepository.LabPerformanceSummaryProjection row : labRows) {
            BigDecimal rev = row.getRevenue()          != null ? row.getRevenue()          : BigDecimal.ZERO;
            long tests     = row.getTestCount()        != null ? row.getTestCount()        : 0L;
            long patients  = row.getPatientCount()     != null ? row.getPatientCount()     : 0L;
            long pending   = row.getPendingSamples()   != null ? row.getPendingSamples()   : 0L;
            long reports   = row.getReportsGenerated() != null ? row.getReportsGenerated() : 0L;

            totalRevenue  = totalRevenue.add(rev);
            totalTests   += tests;
            totalPatients+= patients;
            totalPending += pending;
            totalReports += reports;

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
        cumulative.put("totalLabs",        labRows.size());
        cumulative.put("totalRevenue",     totalRevenue.setScale(2, RoundingMode.HALF_UP));
        cumulative.put("totalTests",       totalTests);
        cumulative.put("totalPatients",    totalPatients);
        cumulative.put("reportsGenerated", totalReports);
        cumulative.put("pendingSamples",   totalPending);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cumulative", cumulative);
        result.put("labWise",    labWise);
        return result;
    }

    private Map<String, Object> buildTestsByCategory(User currentUser, Long userId,
                                                      LocalDate startDate, LocalDate endDate, boolean hasDates, Long labId) {
        List<VisitTestResultRepository.TestsByCategoryDetailedProjection> categories;
        BigDecimal totalPaid, totalDue;

        if (labId != null) {
            if (hasDates) {
                Instant is = toInstantStart(startDate);
                Instant ie = toInstantEnd(endDate);
                categories = visitTestResultRepository.getPatientTestsByCategoryDetailedByLabIdWithDateRange(labId, toStart(startDate), toEnd(endDate));
                totalPaid  = billingRepository.sumPaidAmountByLabId(labId, is, ie);
                totalDue   = billingRepository.sumDueAmountByLabIdAndCreatedAtBetween(labId, is, ie);
            } else {
                categories = visitTestResultRepository.getPatientTestsByCategoryDetailedByLabId(labId);
                totalPaid  = billingRepository.sumPaidAmountByLabIdAllTime(labId);
                totalDue   = billingRepository.sumDueAmountByLabId(labId);
            }
        } else if (hasDates) {
            categories = visitTestResultRepository.getPatientTestsByCategoryDetailedBySuperAdminWithDateRange(userId, toStart(startDate), toEnd(endDate));
            totalPaid  = billingRepository.sumPaidAmountByLabsCreatedByAndCreatedAtBetween(currentUser, toInstantStart(startDate), toInstantEnd(endDate));
            totalDue   = billingRepository.sumDueAmountByLabsCreatedByAndCreatedAtBetween(currentUser, toInstantStart(startDate), toInstantEnd(endDate));
        } else {
            categories = visitTestResultRepository.getPatientTestsByCategoryDetailedBySuperAdmin(userId);
            totalPaid  = billingRepository.sumPaidAmountByLabsCreatedBy(currentUser);
            totalDue   = billingRepository.sumDueAmountByLabsCreatedBy(currentUser);
        }
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        if (totalDue  == null) totalDue  = BigDecimal.ZERO;

        long totalTests      = categories.stream().mapToLong(VisitTestResultRepository.TestsByCategoryDetailedProjection::getTestCount).sum();
        BigDecimal totalDiscount = sumField(categories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getDiscount);
        BigDecimal totalCash     = sumField(categories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getCashRevenue);
        BigDecimal totalUpi      = sumField(categories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getUpiRevenue);
        BigDecimal totalCard     = sumField(categories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getCardRevenue);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTests",    totalTests);
        summary.put("totalRevenue",  totalPaid);
        summary.put("totalDiscount", totalDiscount);
        summary.put("totalPaid",     totalPaid);
        summary.put("totalDue",      totalDue);
        summary.put("totalCash",     totalCash);
        summary.put("totalUpi",      totalUpi);
        summary.put("totalCard",     totalCard);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary",    summary);
        result.put("categories", categories);
        return result;
    }

    private Map<String, Object> buildRevenueTrend(Long userId, LocalDate startDate, LocalDate endDate, Long labId) {
        Instant start = toInstantStart(startDate);
        Instant end   = toInstantEnd(endDate);
        List<BillingRepository.DailyRevenueProjection> trend;
        if (labId != null) {
            trend = billingRepository.getDailyPaidAmountTrendByLabId(labId, start, end);
        } else {
            trend = billingRepository.getDailyPaidAmountTrend(userId, start, end);
        }
        BigDecimal total = trend.stream()
                .map(BillingRepository.DailyRevenueProjection::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRevenue", total);
        result.put("trend",        trend);
        return result;
    }

    private List<?> buildRevenueByLab(Long userId,
                                      LocalDate startDate, LocalDate endDate, boolean hasDates, Long labId) {
        if (labId != null) {
            Optional<BillingRepository.RevenueByLabProjection> opt;
            if (hasDates) {
                opt = billingRepository.getRevenueByLabIdAndDateRange(labId, toInstantStart(startDate), toInstantEnd(endDate));
            } else {
                opt = billingRepository.getRevenueByLabId(labId);
            }
            return opt.map(Collections::singletonList).orElse(Collections.emptyList());
        }
        if (hasDates) {
            return billingRepository.getRevenueByLab(userId, toInstantStart(startDate), toInstantEnd(endDate));
        }
        return billingRepository.getRevenueByLabAllTime(userId);
    }

    private List<Map<String, Object>> buildLabPerformance(Long userId,
                                                           LocalDate startDate, LocalDate endDate, boolean hasDates, int limit, Long labId) {
        List<LabRepository.LabPerformanceSummaryProjection> results;
        if (hasDates) {
            long periodDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            LocalDate prevEnd   = startDate.minusDays(1);
            LocalDate prevStart = prevEnd.minusDays(periodDays - 1);
            results = labRepository.getLabPerformanceSummary(
                    userId, toInstantStart(startDate), toInstantEnd(endDate),
                    toInstantStart(prevStart), toInstantEnd(prevEnd), limit);
        } else {
            results = labRepository.getLabPerformanceSummaryAllTime(userId, limit);
        }

        if (labId != null) {
            final Long filterLabId = labId;
            results = results.stream()
                    .filter(r -> r.getLabId() != null && r.getLabId().equals(filterLabId))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> labs = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            LabRepository.LabPerformanceSummaryProjection row = results.get(i);
            BigDecimal revenue = row.getRevenue()         != null ? row.getRevenue()         : BigDecimal.ZERO;
            BigDecimal prevRev = row.getPreviousRevenue() != null ? row.getPreviousRevenue() : BigDecimal.ZERO;

            Double growthPct = null;
            if (hasDates) {
                growthPct = prevRev.compareTo(BigDecimal.ZERO) > 0
                        ? revenue.subtract(prevRev).divide(prevRev, 4, RoundingMode.HALF_UP)
                                 .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP).doubleValue()
                        : (revenue.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0);
            }

            Map<String, Object> lab = new LinkedHashMap<>();
            lab.put("rank",             i + 1);
            lab.put("labId",            row.getLabId());
            lab.put("labName",          row.getLabName());
            lab.put("revenue",          revenue);
            lab.put("tests",            row.getTestCount());
            lab.put("patients",         row.getPatientCount());
            lab.put("pendingSamples",   row.getPendingSamples());
            lab.put("avgTatHours",      row.getAvgTatHours());
            lab.put("reportsGenerated", row.getReportsGenerated());
            lab.put("growthPct",        growthPct);
            labs.add(lab);
        }
        return labs;
    }

    private List<DoctorRepository.TopReferringDoctorProjection> buildTopReferringDoctors(Long userId,
                                                                                          LocalDate startDate, LocalDate endDate, boolean hasDates, int limit, Long labId) {
        if (labId != null) {
            if (hasDates) {
                return doctorRepository.getTopReferringDoctorsByLabIdWithDateRange(labId, toInstantStart(startDate), toInstantEnd(endDate), limit);
            }
            return doctorRepository.getTopReferringDoctorsByLabId(labId, limit);
        }
        if (hasDates) {
            return doctorRepository.getTopReferringDoctorsWithDateRange(userId, toInstantStart(startDate), toInstantEnd(endDate), limit);
        }
        return doctorRepository.getTopReferringDoctors(userId, limit);
    }

    private Map<String, Object> buildDetailedBilling(Long userId,
                                                      LocalDate startDate, LocalDate endDate, boolean hasDates, Long labId) {
        List<BillingRepository.DetailedBillingSummaryProjection> summaryList;
        List<BillingRepository.BillingByStatusProjection> byStatus;
        List<VisitTestResultRepository.TestsByCategoryDetailedProjection> testCategories;
        List<HealthPackageRepository.PackageSummaryProjection> packages;

        if (labId != null) {
            if (hasDates) {
                Instant iStart = toInstantStart(startDate);
                Instant iEnd   = toInstantEnd(endDate);
                summaryList    = billingRepository.getDetailedBillingSummaryByLabIdWithDateRange(labId, iStart, iEnd);
                byStatus       = billingRepository.getBillingByStatusByLabIdWithDateRange(labId, iStart, iEnd);
                testCategories = visitTestResultRepository.getPatientTestsByCategoryDetailedByLabIdWithDateRange(labId, toStart(startDate), toEnd(endDate));
                packages       = healthPackageRepository.getPackageSummaryByLabIdWithDateRange(labId, iStart, iEnd);
            } else {
                summaryList    = billingRepository.getDetailedBillingSummaryByLabId(labId);
                byStatus       = billingRepository.getBillingByStatusByLabId(labId);
                testCategories = visitTestResultRepository.getPatientTestsByCategoryDetailedByLabId(labId);
                packages       = healthPackageRepository.getPackageSummaryByLabId(labId);
            }
        } else if (hasDates) {
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

        BigDecimal tcRevenue  = sumField(testCategories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getRevenue);
        BigDecimal tcDiscount = sumField(testCategories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getDiscount);
        BigDecimal tcPaid     = sumField(testCategories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getPaidRevenue);
        BigDecimal tcDue      = sumField(testCategories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getDueRevenue);

        Map<String, Object> testsSummary = new LinkedHashMap<>();
        testsSummary.put("totalCategories", testCategories.size());
        testsSummary.put("totalTests",  testCategories.stream().mapToLong(VisitTestResultRepository.TestsByCategoryDetailedProjection::getTestCount).sum());
        testsSummary.put("grossBilled", tcRevenue);
        testsSummary.put("discount",    tcDiscount);
        testsSummary.put("paid",        tcPaid);
        testsSummary.put("due",         tcDue);
        testsSummary.put("paymentMode", Map.of(
                "cash", sumField(testCategories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getCashRevenue),
                "upi",  sumField(testCategories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getUpiRevenue),
                "card", sumField(testCategories, VisitTestResultRepository.TestsByCategoryDetailedProjection::getCardRevenue)));

        BigDecimal pkgPaid = sumField(packages, HealthPackageRepository.PackageSummaryProjection::getPaidRevenue);
        BigDecimal pkgDue  = sumField(packages, HealthPackageRepository.PackageSummaryProjection::getDueRevenue);

        Map<String, Object> packageSummary = new LinkedHashMap<>();
        packageSummary.put("totalPackages", packages.size());
        packageSummary.put("totalVisits",   packages.stream().mapToLong(p -> p.getVisitCount() != null ? p.getVisitCount() : 0L).sum());
        packageSummary.put("grossBilled",   sumField(packages, HealthPackageRepository.PackageSummaryProjection::getRevenue));
        packageSummary.put("discount",      sumField(packages, HealthPackageRepository.PackageSummaryProjection::getDiscount));
        packageSummary.put("paid",          pkgPaid);
        packageSummary.put("due",           pkgDue);
        packageSummary.put("paymentMode",   Map.of(
                "cash", sumField(packages, HealthPackageRepository.PackageSummaryProjection::getCashRevenue),
                "upi",  sumField(packages, HealthPackageRepository.PackageSummaryProjection::getUpiRevenue),
                "card", sumField(packages, HealthPackageRepository.PackageSummaryProjection::getCardRevenue)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary",         summary);
        result.put("byPaymentStatus", statusBreakdown);
        result.put("testsSummary",    testsSummary);
        result.put("testCategories",  testCategories);
        result.put("packageSummary",  packageSummary);
        result.put("packages",        packages);
        return result;
    }

    private Map<String, Object> buildPackagesSummary(Long userId,
                                                      LocalDate startDate, LocalDate endDate, boolean hasDates, Long labId) {
        List<HealthPackageRepository.PackageSummaryProjection> packages;

        if (labId != null) {
            if (hasDates) {
                packages = healthPackageRepository.getPackageSummaryByLabIdWithDateRange(labId, toInstantStart(startDate), toInstantEnd(endDate));
            } else {
                packages = healthPackageRepository.getPackageSummaryByLabId(labId);
            }
        } else if (hasDates) {
            packages = healthPackageRepository.getPackageSummaryBySuperAdminWithDateRange(userId, toInstantStart(startDate), toInstantEnd(endDate));
        } else {
            packages = healthPackageRepository.getPackageSummaryBySuperAdmin(userId);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalPackages",  packages.size());
        summary.put("totalVisits",    packages.stream().mapToLong(p -> p.getVisitCount() != null ? p.getVisitCount() : 0L).sum());
        summary.put("totalRevenue",   sumField(packages, HealthPackageRepository.PackageSummaryProjection::getRevenue));
        summary.put("totalDiscount",  sumField(packages, HealthPackageRepository.PackageSummaryProjection::getDiscount));
        summary.put("totalPaid",      sumField(packages, HealthPackageRepository.PackageSummaryProjection::getPaidRevenue));
        summary.put("totalDue",       sumField(packages, HealthPackageRepository.PackageSummaryProjection::getDueRevenue));
        summary.put("totalCash",      sumField(packages, HealthPackageRepository.PackageSummaryProjection::getCashRevenue));
        summary.put("totalUpi",       sumField(packages, HealthPackageRepository.PackageSummaryProjection::getUpiRevenue));
        summary.put("totalCard",      sumField(packages, HealthPackageRepository.PackageSummaryProjection::getCardRevenue));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary",  summary);
        result.put("packages", packages);
        return result;
    }

    private Map<String, Object> buildEarningsByCategory(User currentUser,
                                                         LocalDate startDate, LocalDate endDate, boolean hasDates, Long labId) {
        List<VisitTestResultRepository.TestEarningsByTestProjection> rows;

        if (labId != null) {
            if (hasDates) {
                rows = visitTestResultRepository.getEarningsByTestByLabIdWithDateRange(labId, toStart(startDate), toEnd(endDate));
            } else {
                rows = visitTestResultRepository.getEarningsByTestByLabId(labId);
            }
        } else if (hasDates) {
            rows = visitTestResultRepository.getEarningsByTestBySuperAdminWithDateRange(
                    currentUser.getId(), toStart(startDate), toEnd(endDate));
        } else {
            rows = visitTestResultRepository.getEarningsByTestBySuperAdmin(currentUser.getId());
        }

        Map<String, List<VisitTestResultRepository.TestEarningsByTestProjection>> byCategory = new LinkedHashMap<>();
        for (VisitTestResultRepository.TestEarningsByTestProjection row : rows) {
            byCategory.computeIfAbsent(row.getCategory(), k -> new ArrayList<>()).add(row);
        }

        BigDecimal grandTotalRevenue = BigDecimal.ZERO;
        BigDecimal grandTotalDue     = BigDecimal.ZERO;
        long       grandTotalTests   = 0;

        List<Map<String, Object>> categories = new ArrayList<>();
        for (Map.Entry<String, List<VisitTestResultRepository.TestEarningsByTestProjection>> entry : byCategory.entrySet()) {
            BigDecimal catRevenue = BigDecimal.ZERO;
            BigDecimal catDue     = BigDecimal.ZERO;
            long       catCount   = 0;

            List<Map<String, Object>> tests = new ArrayList<>();
            for (VisitTestResultRepository.TestEarningsByTestProjection t : entry.getValue()) {
                BigDecimal te = safe(t.getTotalEarnings());
                BigDecimal tp = safe(t.getPaidAmount());
                BigDecimal td = safe(t.getDueAmount());
                long       tc = t.getOrderedCount() != null ? t.getOrderedCount() : 0L;

                catRevenue = catRevenue.add(tp);
                catDue     = catDue.add(td);
                catCount  += tc;

                Map<String, Object> testMap = new LinkedHashMap<>();
                testMap.put("testId",        t.getTestId());
                testMap.put("testName",      t.getTestName());
                testMap.put("testCode",      t.getTestCode());
                testMap.put("price",         safe(t.getTestPrice()));
                testMap.put("orderedCount",  tc);
                testMap.put("grossEarnings", te);
                testMap.put("revenue",       tp);
                testMap.put("dueAmount",     td);
                tests.add(testMap);
            }

            grandTotalRevenue = grandTotalRevenue.add(catRevenue);
            grandTotalDue     = grandTotalDue.add(catDue);
            grandTotalTests  += catCount;

            Map<String, Object> catMap = new LinkedHashMap<>();
            catMap.put("category",   entry.getKey());
            catMap.put("totalTests", catCount);
            catMap.put("revenue",    catRevenue.setScale(2, RoundingMode.HALF_UP));
            catMap.put("dueAmount",  catDue.setScale(2, RoundingMode.HALF_UP));
            catMap.put("tests",      tests);
            categories.add(catMap);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCategories", categories.size());
        summary.put("totalTests",      grandTotalTests);
        summary.put("totalRevenue",    grandTotalRevenue.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalDue",        grandTotalDue.setScale(2, RoundingMode.HALF_UP));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary",    summary);
        result.put("categories", categories);
        return result;
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

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

    private LocalDateTime toStart(LocalDate date) { return date.atStartOfDay(); }
    private LocalDateTime toEnd(LocalDate date)   { return date.atTime(LocalTime.MAX); }
    private Instant toInstantStart(LocalDate date) { return date.atStartOfDay(ZoneId.systemDefault()).toInstant(); }
    private Instant toInstantEnd(LocalDate date)   { return date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant(); }
}
