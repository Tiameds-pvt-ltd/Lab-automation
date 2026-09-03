package tiameds.com.tiameds.services.lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Keeps daily_lab_stats (the super-admin dashboard rollup) in sync one (lab, day) at a time.
 * Deliberately reuses the SAME filtered repository queries the live dashboard
 * (SuperAdminDashboardController) already uses for a given lab+date-range, so rollup numbers
 * are guaranteed to match what the dashboard currently computes live — no new filter logic
 * to drift out of sync with existing "cancelled visit" / "active test" / "Completed report" rules.
 *
 * recomputeDay() re-aggregates the whole day from source every time it's called (not an
 * increment), so it self-corrects regardless of whether it's triggered by a create, an update
 * (e.g. partial payment), or a status change (e.g. report marked Completed) — it can't drift
 * from double-counting.
 *
 * Callers MUST NOT let a failure here fail their own transaction: dashboard staleness is
 * recoverable (rerun for that lab+day), a failed billing/visit/report save is not. Hence the
 * try/catch here — this method never throws.
 */
@Service
public class DashboardRollupService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardRollupService.class);

    private final BillingRepository billingRepository;
    private final VisitRepository visitRepository;
    private final VisitTestResultRepository visitTestResultRepository;
    private final DailyLabStatsRepository dailyLabStatsRepository;

    public DashboardRollupService(BillingRepository billingRepository,
                                   VisitRepository visitRepository,
                                   VisitTestResultRepository visitTestResultRepository,
                                   DailyLabStatsRepository dailyLabStatsRepository) {
        this.billingRepository = billingRepository;
        this.visitRepository = visitRepository;
        this.visitTestResultRepository = visitTestResultRepository;
        this.dailyLabStatsRepository = dailyLabStatsRepository;
    }

    public void recomputeDay(Long labId, LocalDate date) {
        if (labId == null || date == null) {
            return;
        }
        try {
            Instant instantStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant instantEnd = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();
            LocalDateTime ldtStart = date.atStartOfDay();
            LocalDateTime ldtEnd = date.atTime(LocalTime.MAX);

            BigDecimal paidRevenue = billingRepository.sumPaidAmountByLabId(labId, instantStart, instantEnd);
            BigDecimal dueRevenue = billingRepository.sumDueAmountByLabIdAndCreatedAtBetween(labId, instantStart, instantEnd);
            long testCount = visitTestResultRepository.countAllTestsByLabIdAndCreatedAtBetween(labId, ldtStart, ldtEnd);
            long reportsGenerated = visitTestResultRepository.countCompletedReportsByLabIdAndCreatedAtBetween(labId, ldtStart, ldtEnd);
            long pendingSamples = visitRepository.countPendingVisitsByLabIdAndCreatedAtBetween(labId, instantStart, instantEnd);
            long patientCount = visitRepository.countDistinctPatientsByLabIdAndCreatedAtBetween(labId, instantStart, instantEnd);

            dailyLabStatsRepository.upsertRow(
                    labId,
                    date,
                    testCount,
                    reportsGenerated,
                    pendingSamples,
                    patientCount,
                    paidRevenue != null ? paidRevenue : BigDecimal.ZERO,
                    dueRevenue != null ? dueRevenue : BigDecimal.ZERO);
        } catch (Exception e) {
            logger.error("daily_lab_stats rollup failed for labId={}, date={} — dashboard may show stale data for this day until re-run", labId, date, e);
        }
    }
}
