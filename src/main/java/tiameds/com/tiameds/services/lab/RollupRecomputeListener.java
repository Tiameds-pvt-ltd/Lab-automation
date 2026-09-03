package tiameds.com.tiameds.services.lab;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Runs DashboardRollupService.recomputeDay off the request thread, after the
 * publishing write's own transaction has committed — so a write's latency no
 * longer includes the rollup's ~6 extra queries, and the rollup can never update
 * based on a change that ultimately rolled back.
 *
 * fallbackExecution = true: ReportService.createReports isn't @Transactional, so
 * there's no active transaction to wait on when it publishes — without this flag
 * the event would be silently dropped. With it, the listener still runs (still
 * asynchronously, since @Async is independent of @TransactionalEventListener)
 * immediately in that case instead of waiting for a commit that will never fire.
 */
@Component
public class RollupRecomputeListener {

    private final DashboardRollupService dashboardRollupService;

    public RollupRecomputeListener(DashboardRollupService dashboardRollupService) {
        this.dashboardRollupService = dashboardRollupService;
    }

    @Async("rollupTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRollupRecompute(RollupRecomputeEvent event) {
        dashboardRollupService.recomputeDay(event.getLabId(), event.getDate());
    }
}
