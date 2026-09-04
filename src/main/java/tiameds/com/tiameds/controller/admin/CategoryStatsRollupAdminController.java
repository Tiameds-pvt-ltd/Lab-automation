package tiameds.com.tiameds.controller.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tiameds.com.tiameds.entity.User;
import tiameds.com.tiameds.services.lab.CategoryStatsBackfillService;
import tiameds.com.tiameds.utils.ApiResponseHelper;
import tiameds.com.tiameds.utils.UserAuthService;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * On-demand backfill for daily_lab_category_stats. Same pattern as
 * DashboardRollupAdminController, kept in its own controller so it can be
 * deployed/tested independently of the existing daily_lab_stats rollup.
 */
@RestController
@RequestMapping("/lab-admin/stats/category-rollup")
@Tag(name = "Category Stats Rollup Admin Controller", description = "On-demand backfill for the daily_lab_category_stats rollup")
public class CategoryStatsRollupAdminController {

    private final CategoryStatsBackfillService categoryStatsBackfillService;
    private final UserAuthService userAuthService;

    public CategoryStatsRollupAdminController(CategoryStatsBackfillService categoryStatsBackfillService,
                                               UserAuthService userAuthService) {
        this.categoryStatsBackfillService = categoryStatsBackfillService;
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
                ? categoryStatsBackfillService.backfillLab(labId, startDate, endDate)
                : categoryStatsBackfillService.backfillAllLabs(startDate, endDate);

        return ApiResponseHelper.successResponse("Category stats rollup backfill completed",
                Map.of("labId", labId, "startDate", startDate, "endDate", endDate, "dayRowsProcessed", dayRows));
    }
}
