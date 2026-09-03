package tiameds.com.tiameds.controller.superAdmin;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.lab.DashboardRollupBackfillService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.UserAuthService;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * On-demand backfill/repair for the daily_lab_stats dashboard rollup table.
 * Not run automatically on startup — an authenticated super admin triggers it explicitly,
 * once after deploying the rollup (V17 migration), or again for any date range if numbers
 * ever need to be re-synced (e.g. after fixing a bug in the rollup's aggregation logic).
 * Purely additive/idempotent: only reads existing billing/visit/report data and
 * inserts/updates rows in daily_lab_stats — never touches existing tables.
 */
@RestController
@RequestMapping("/lab-super-admin/stats/rollup")
@Tag(name = "Dashboard Rollup Admin Controller", description = "On-demand backfill for the daily_lab_stats dashboard rollup")
public class DashboardRollupAdminController {

    private final DashboardRollupBackfillService dashboardRollupBackfillService;
    private final UserAuthService userAuthService;

    public DashboardRollupAdminController(DashboardRollupBackfillService dashboardRollupBackfillService,
                                          UserAuthService userAuthService) {
        this.dashboardRollupBackfillService = dashboardRollupBackfillService;
        this.userAuthService = userAuthService;
    }

    @PostMapping("/backfill")
    public ResponseEntity<?> backfill(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Long labId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        Optional<User> userOptional = userAuthService.authenticateUser(token);
        if (userOptional.isEmpty()) {
            return ApiResponseHelper.errorResponse("User authentication failed", HttpStatus.UNAUTHORIZED);
        }
        if (endDate.isBefore(startDate)) {
            return ApiResponseHelper.errorResponse("endDate must not be before startDate", HttpStatus.BAD_REQUEST);
        }

        int dayRows = (labId != null)
                ? dashboardRollupBackfillService.backfillLab(labId, startDate, endDate)
                : dashboardRollupBackfillService.backfillAllLabs(startDate, endDate);

        return ApiResponseHelper.successResponse("Dashboard rollup backfill completed",
                Map.of("labId", labId, "startDate", startDate, "endDate", endDate, "dayRowsProcessed", dayRows));
    }
}
