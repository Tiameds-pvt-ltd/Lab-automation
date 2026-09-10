package tiameds.com.tiameds.services.lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.repository.DailyLabCategoryStatsRepository;
import tiameds.com.tiameds.repository.DailyLabStatsRepository;
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
    private final DailyLabStatsRepository dailyLabStatsRepository;
    private final DailyLabCategoryStatsRepository dailyLabCategoryStatsRepository;

    public RollupStartupBackfillRunner(LabRepository labRepository,
                                        DashboardRollupBackfillService dashboardRollupBackfillService,
                                        CategoryStatsBackfillService categoryStatsBackfillService,
                                        DailyLabStatsRepository dailyLabStatsRepository,
                                        DailyLabCategoryStatsRepository dailyLabCategoryStatsRepository) {
        this.labRepository = labRepository;
        this.dashboardRollupBackfillService = dashboardRollupBackfillService;
        this.categoryStatsBackfillService = categoryStatsBackfillService;
        this.dailyLabStatsRepository = dailyLabStatsRepository;
        this.dailyLabCategoryStatsRepository = dailyLabCategoryStatsRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Lab> labs = labRepository.findAll();
        LocalDate today = LocalDate.now();
        logger.info("Startup rollup backfill: scheduling {} lab(s) in background thread", labs.size());

        // Run in a background daemon thread so the main thread (and therefore Tomcat's request
        // handling) is never blocked while the backfill runs. Without this, the backfill runs
        // on the main thread for 20+ minutes on startup, causing the RDS instance to be under
        // heavy write load at the exact moment the first API requests arrive. Because the
        // /tests-by-category endpoint uses the same underlying query that recomputeDay() fires
        // for every lab-day, those API reads compete with hundreds of in-flight backfill queries
        // and breach the AWS ALB's 60-second idle timeout → 504 Gateway Time-out.
        //
        // Daemon=true: the JVM can still exit cleanly if the app is stopped mid-backfill.
        Thread backfillThread = new Thread(() -> {
            logger.info("Startup rollup backfill: starting for {} lab(s)", labs.size());
            for (Lab lab : labs) {
                try {
                    LocalDate labCreated = lab.getCreatedAt() != null ? lab.getCreatedAt().toLocalDate() : today;

                    // Only recompute days that are missing from the rollup tables. After the first
                    // full backfill this reduces from O(labs × days) down to O(labs) — each restart
                    // only touches "today" per lab instead of hundreds of historical days. Using the
                    // last stored date (not lastDate+1) so that date is recomputed too, catching any
                    // partial data that may have been written mid-day on the previous run.
                    LocalDate dashboardMaxDate = dailyLabStatsRepository.findMaxStatDateByLabId(lab.getId());
                    LocalDate dashboardFrom = dashboardMaxDate != null ? dashboardMaxDate : labCreated;

                    LocalDate categoryMaxDate = dailyLabCategoryStatsRepository.findMaxStatDateByLabId(lab.getId());
                    LocalDate categoryFrom = categoryMaxDate != null ? categoryMaxDate : labCreated;

                    dashboardRollupBackfillService.backfillLab(lab.getId(), dashboardFrom, today);
                    categoryStatsBackfillService.backfillLab(lab.getId(), categoryFrom, today);
                } catch (IllegalStateException e) {
                    // EntityManagerFactory closed = JVM shutting down; stop immediately.
                    logger.warn("Startup rollup backfill: stopping early — application is shutting down (reached labId={})", lab.getId());
                    return;
                } catch (Exception e) {
                    logger.error("Startup rollup backfill failed for labId={} — dashboard may show stale data until a manual backfill is run", lab.getId(), e);
                }
            }
            logger.info("Startup rollup backfill: completed for {} lab(s)", labs.size());
        }, "startup-rollup-backfill");
        backfillThread.setDaemon(true);
        backfillThread.start();
    }
}
