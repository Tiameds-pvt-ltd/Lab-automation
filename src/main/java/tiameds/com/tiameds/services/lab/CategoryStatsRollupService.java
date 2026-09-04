package tiameds.com.tiameds.services.lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tiameds.com.tiameds.repository.DailyLabCategoryStatsRepository;
import tiameds.com.tiameds.repository.VisitTestResultRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Keeps daily_lab_category_stats (per-category tests/revenue breakdown) in sync,
 * one (lab, day) at a time. Completely independent of DashboardRollupService /
 * daily_lab_stats — a separate table, separate listener (see
 * CategoryStatsRollupListener), separate failure domain. A bug here must never
 * be able to affect the existing daily_lab_stats rollup or any write path.
 *
 * Reuses the same getPatientTestsByCategoryDetailedByLabIdWithDateRange query the
 * live dashboards already call, so rollup numbers match what live queries would
 * have returned for that day — no new filter logic to drift out of sync.
 */
@Service
public class CategoryStatsRollupService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryStatsRollupService.class);

    private final VisitTestResultRepository visitTestResultRepository;
    private final DailyLabCategoryStatsRepository dailyLabCategoryStatsRepository;

    public CategoryStatsRollupService(VisitTestResultRepository visitTestResultRepository,
                                       DailyLabCategoryStatsRepository dailyLabCategoryStatsRepository) {
        this.visitTestResultRepository = visitTestResultRepository;
        this.dailyLabCategoryStatsRepository = dailyLabCategoryStatsRepository;
    }

    public void recomputeDay(Long labId, LocalDate date) {
        if (labId == null || date == null) {
            return;
        }
        try {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            var categories = visitTestResultRepository.getPatientTestsByCategoryDetailedByLabIdWithDateRange(labId, start, end);

            // Whole-day re-aggregation: clear stale categories first so one that had
            // tests yesterday but none today doesn't keep showing a leftover row.
            dailyLabCategoryStatsRepository.deleteByLabIdAndStatDate(labId, date);

            for (var row : categories) {
                dailyLabCategoryStatsRepository.upsertRow(
                        labId,
                        date,
                        row.getCategory(),
                        row.getTestCount() != null ? row.getTestCount() : 0L,
                        safe(row.getRevenue()),
                        safe(row.getDiscount()),
                        safe(row.getPaidRevenue()),
                        safe(row.getDueRevenue()),
                        safe(row.getCashRevenue()),
                        safe(row.getUpiRevenue()),
                        safe(row.getCardRevenue()));
            }
        } catch (Exception e) {
            logger.error("daily_lab_category_stats rollup failed for labId={}, date={} — category dashboard may show stale data for this day until re-run", labId, date, e);
        }
    }

    private static java.math.BigDecimal safe(java.math.BigDecimal val) {
        return val != null ? val : java.math.BigDecimal.ZERO;
    }
}
