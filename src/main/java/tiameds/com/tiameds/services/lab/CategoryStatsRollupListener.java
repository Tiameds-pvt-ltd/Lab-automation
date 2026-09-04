package tiameds.com.tiameds.services.lab;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * A second, independent subscriber to the existing RollupRecomputeEvent
 * (already published by VisitService/BillingManagementService/ReportService/
 * PatientService/UpdatePatientService for daily_lab_stats). Spring supports
 * multiple listeners per event, so this required zero changes to any write
 * path or to the existing RollupRecomputeListener/DashboardRollupService —
 * this class is purely additive.
 *
 * Reuses the same "rollupTaskExecutor" thread pool/async semantics as the
 * existing listener; a failure or slowness here cannot block a write's own
 * transaction (already committed by the time this runs) and cannot affect
 * the existing daily_lab_stats rollup, since CategoryStatsRollupService
 * never throws and touches a completely separate table.
 */
@Component
public class CategoryStatsRollupListener {

    private final CategoryStatsRollupService categoryStatsRollupService;

    public CategoryStatsRollupListener(CategoryStatsRollupService categoryStatsRollupService) {
        this.categoryStatsRollupService = categoryStatsRollupService;
    }

    @Async("rollupTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRollupRecompute(RollupRecomputeEvent event) {
        categoryStatsRollupService.recomputeDay(event.getLabId(), event.getDate());
    }
}
