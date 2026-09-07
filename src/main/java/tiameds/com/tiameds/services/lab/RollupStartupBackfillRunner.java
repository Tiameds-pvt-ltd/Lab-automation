package tiameds.com.tiameds.services.lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.repository.LabRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Runs once, automatically, every time the application starts. Backfills both
 * daily_lab_stats and daily_lab_category_stats for every lab, covering
 * [lab.createdAt, today] — so a fresh/empty database (or a restart after any
 * gap, e.g. rollup writes missed during downtime) always has rollup data
 * ready before the first dashboard request, with no manual admin call needed.
 *
 * Deliberately reuses the existing DashboardRollupBackfillService /
 * CategoryStatsBackfillService — the same idempotent, per-day
 * recompute-from-source logic already used by the manual backfill endpoints
 * (POST /lab-super-admin/stats/rollup/backfill and
 * POST /lab-admin/stats/category-rollup/backfill) — so startup behavior can
 * never drift from what an admin would get by calling those endpoints
 * by hand. Per-lab failures are caught and logged so one bad lab can't stop
 * the rest of startup, or the application itself, from proceeding.
 */
@Component
public class RollupStartupBackfillRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(RollupStartupBackfillRunner.class);

    private final LabRepository labRepository;
    private final DashboardRollupBackfillService dashboardRollupBackfillService;
    private final CategoryStatsBackfillService categoryStatsBackfillService;

    public RollupStartupBackfillRunner(LabRepository labRepository,
                                        DashboardRollupBackfillService dashboardRollupBackfillService,
                                        CategoryStatsBackfillService categoryStatsBackfillService) {
        this.labRepository = labRepository;
        this.dashboardRollupBackfillService = dashboardRollupBackfillService;
        this.categoryStatsBackfillService = categoryStatsBackfillService;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Lab> labs = labRepository.findAll();
        LocalDate today = LocalDate.now();
        logger.info("Startup rollup backfill: starting for {} lab(s)", labs.size());

        for (Lab lab : labs) {
            try {
                LocalDate startDate = lab.getCreatedAt() != null ? lab.getCreatedAt().toLocalDate() : today;
                dashboardRollupBackfillService.backfillLab(lab.getId(), startDate, today);
                categoryStatsBackfillService.backfillLab(lab.getId(), startDate, today);
            } catch (Exception e) {
                logger.error("Startup rollup backfill failed for labId={} — dashboard may show stale data until a manual backfill is run", lab.getId(), e);
            }
        }

        logger.info("Startup rollup backfill: completed for {} lab(s)", labs.size());
    }
}
