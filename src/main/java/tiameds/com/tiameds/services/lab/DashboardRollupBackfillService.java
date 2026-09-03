package tiameds.com.tiameds.services.lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.repository.LabRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * One-time / on-demand backfill for daily_lab_stats. Pure read against existing
 * billing/patient_visits/visit_test_result data + upserts into the new rollup table only —
 * never touches existing tables, safe to re-run for any date range at any time (idempotent),
 * safe to run against production without a maintenance window.
 *
 * Deliberately reuses DashboardRollupService.recomputeDay(labId, date) — the exact same
 * per-day aggregation the live write-path hooks use — so backfilled rows and incrementally
 * maintained rows can never disagree on filter semantics.
 */
@Service
public class DashboardRollupBackfillService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardRollupBackfillService.class);

    private final LabRepository labRepository;
    private final DashboardRollupService dashboardRollupService;

    public DashboardRollupBackfillService(LabRepository labRepository, DashboardRollupService dashboardRollupService) {
        this.labRepository = labRepository;
        this.dashboardRollupService = dashboardRollupService;
    }

    /** Recomputes every day in [startDate, endDate] (inclusive) for one lab. */
    public int backfillLab(Long labId, LocalDate startDate, LocalDate endDate) {
        int days = 0;
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            dashboardRollupService.recomputeDay(labId, d);
            days++;
        }
        logger.info("Backfilled daily_lab_stats for labId={} from {} to {} ({} days)", labId, startDate, endDate, days);
        return days;
    }

    /** Recomputes every day in [startDate, endDate] (inclusive) for every lab in the system. */
    public int backfillAllLabs(LocalDate startDate, LocalDate endDate) {
        List<Lab> labs = labRepository.findAll();
        int totalDayRows = 0;
        for (Lab lab : labs) {
            totalDayRows += backfillLab(lab.getId(), startDate, endDate);
        }
        logger.info("Backfilled daily_lab_stats for {} labs from {} to {}", labs.size(), startDate, endDate);
        return totalDayRows;
    }
}
