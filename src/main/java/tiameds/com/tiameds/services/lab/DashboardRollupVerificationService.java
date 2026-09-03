package tiameds.com.tiameds.services.lab;

import org.springframework.stereotype.Service;
import tiameds.com.tiameds.repository.BillingRepository;
import tiameds.com.tiameds.repository.DailyLabStatsRepository;
import tiameds.com.tiameds.repository.VisitRepository;
import tiameds.com.tiameds.repository.VisitTestResultRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only comparison of daily_lab_stats (rollup) vs. live-aggregated totals for a
 * lab + date range, used to confirm the rollup can be trusted before cutting the
 * dashboard's reads over to it. Never writes anything — safe to call anytime.
 */
@Service
public class DashboardRollupVerificationService {

    private final DailyLabStatsRepository dailyLabStatsRepository;
    private final BillingRepository billingRepository;
    private final VisitRepository visitRepository;
    private final VisitTestResultRepository visitTestResultRepository;

    public DashboardRollupVerificationService(DailyLabStatsRepository dailyLabStatsRepository,
                                               BillingRepository billingRepository,
                                               VisitRepository visitRepository,
                                               VisitTestResultRepository visitTestResultRepository) {
        this.dailyLabStatsRepository = dailyLabStatsRepository;
        this.billingRepository = billingRepository;
        this.visitRepository = visitRepository;
        this.visitTestResultRepository = visitTestResultRepository;
    }

    public Map<String, Object> verify(Long labId, LocalDate startDate, LocalDate endDate) {
        DailyLabStatsRepository.RangeSummaryProjection rollup = dailyLabStatsRepository.sumRangeForLab(labId, startDate, endDate);

        Instant instantStart = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant instantEnd = endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();
        LocalDateTime ldtStart = startDate.atStartOfDay();
        LocalDateTime ldtEnd = endDate.atTime(LocalTime.MAX);

        BigDecimal livePaid = billingRepository.sumPaidAmountByLabId(labId, instantStart, instantEnd);
        BigDecimal liveDue = billingRepository.sumDueAmountByLabIdAndCreatedAtBetween(labId, instantStart, instantEnd);
        long liveTestCount = visitTestResultRepository.countAllTestsByLabIdAndCreatedAtBetween(labId, ldtStart, ldtEnd);
        long liveReports = visitTestResultRepository.countCompletedReportsByLabIdAndCreatedAtBetween(labId, ldtStart, ldtEnd);
        long livePending = visitRepository.countPendingVisitsByLabIdAndCreatedAtBetween(labId, instantStart, instantEnd);
        long livePatients = visitRepository.countDistinctPatientsByLabIdAndCreatedAtBetween(labId, instantStart, instantEnd);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labId", labId);
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        Map<String, Object> rollupMap = new LinkedHashMap<>();
        rollupMap.put("testCount", rollup.getTestCount());
        rollupMap.put("reportsGenerated", rollup.getReportsGenerated());
        rollupMap.put("pendingSamples", rollup.getPendingSamples());
        rollupMap.put("patientCount", rollup.getPatientCount());
        rollupMap.put("paidRevenue", rollup.getPaidRevenue());
        rollupMap.put("dueRevenue", rollup.getDueRevenue());
        result.put("rollup", rollupMap);

        Map<String, Object> liveMap = new LinkedHashMap<>();
        liveMap.put("testCount", liveTestCount);
        liveMap.put("reportsGenerated", liveReports);
        liveMap.put("pendingSamples", livePending);
        liveMap.put("patientCount", livePatients);
        liveMap.put("paidRevenue", livePaid != null ? livePaid : BigDecimal.ZERO);
        liveMap.put("dueRevenue", liveDue != null ? liveDue : BigDecimal.ZERO);
        result.put("live", liveMap);

        boolean matches =
                rollup.getTestCount().equals(liveTestCount)
                        && rollup.getReportsGenerated().equals(liveReports)
                        && rollup.getPendingSamples().equals(livePending)
                        && rollup.getPatientCount().equals(livePatients)
                        && bigDecimalEquals(rollup.getPaidRevenue(), livePaid)
                        && bigDecimalEquals(rollup.getDueRevenue(), liveDue);
        result.put("matches", matches);

        return result;
    }

    private boolean bigDecimalEquals(BigDecimal a, BigDecimal b) {
        BigDecimal x = a != null ? a : BigDecimal.ZERO;
        BigDecimal y = b != null ? b : BigDecimal.ZERO;
        return x.compareTo(y) == 0;
    }
}
