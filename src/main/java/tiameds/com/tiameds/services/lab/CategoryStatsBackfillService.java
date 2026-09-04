package tiameds.com.tiameds.services.lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tiameds.com.tiameds.entity.Lab;
import tiameds.com.tiameds.repository.LabRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * One-time / on-demand backfill for daily_lab_category_stats. Mirrors
 * DashboardRollupBackfillService's pattern exactly, but targets the separate
 * category rollup table/service — never touches daily_lab_stats or its
 * backfill/verification services.
 */
@Service
public class CategoryStatsBackfillService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryStatsBackfillService.class);

    private final LabRepository labRepository;
    private final CategoryStatsRollupService categoryStatsRollupService;

    public CategoryStatsBackfillService(LabRepository labRepository, CategoryStatsRollupService categoryStatsRollupService) {
        this.labRepository = labRepository;
        this.categoryStatsRollupService = categoryStatsRollupService;
    }

    public int backfillLab(Long labId, LocalDate startDate, LocalDate endDate) {
        int days = 0;
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            categoryStatsRollupService.recomputeDay(labId, d);
            days++;
        }
        logger.info("Backfilled daily_lab_category_stats for labId={} from {} to {} ({} days)", labId, startDate, endDate, days);
        return days;
    }

    public int backfillAllLabs(LocalDate startDate, LocalDate endDate) {
        List<Lab> labs = labRepository.findAll();
        int totalDayRows = 0;
        for (Lab lab : labs) {
            totalDayRows += backfillLab(lab.getId(), startDate, endDate);
        }
        logger.info("Backfilled daily_lab_category_stats for {} labs from {} to {}", labs.size(), startDate, endDate);
        return totalDayRows;
    }
}
