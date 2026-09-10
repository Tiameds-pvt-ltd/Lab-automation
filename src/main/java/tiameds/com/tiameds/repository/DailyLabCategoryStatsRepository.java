package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.DailyLabCategoryStats;
import tiameds.com.tiameds.entity.DailyLabCategoryStatsId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DailyLabCategoryStatsRepository extends JpaRepository<DailyLabCategoryStats, DailyLabCategoryStatsId> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO daily_lab_category_stats " +
            "(lab_id, stat_date, category, test_count, gross_revenue, discount, paid_revenue, due_revenue, cash_revenue, upi_revenue, card_revenue, updated_at) " +
            "VALUES (:labId, :statDate, :category, :testCount, :grossRevenue, :discount, :paidRevenue, :dueRevenue, :cashRevenue, :upiRevenue, :cardRevenue, now()) " +
            "ON CONFLICT (lab_id, stat_date, category) DO UPDATE SET " +
            "test_count = EXCLUDED.test_count, " +
            "gross_revenue = EXCLUDED.gross_revenue, " +
            "discount = EXCLUDED.discount, " +
            "paid_revenue = EXCLUDED.paid_revenue, " +
            "due_revenue = EXCLUDED.due_revenue, " +
            "cash_revenue = EXCLUDED.cash_revenue, " +
            "upi_revenue = EXCLUDED.upi_revenue, " +
            "card_revenue = EXCLUDED.card_revenue, " +
            "updated_at = now()",
            nativeQuery = true)
    void upsertRow(@Param("labId") Long labId,
                    @Param("statDate") LocalDate statDate,
                    @Param("category") String category,
                    @Param("testCount") long testCount,
                    @Param("grossRevenue") BigDecimal grossRevenue,
                    @Param("discount") BigDecimal discount,
                    @Param("paidRevenue") BigDecimal paidRevenue,
                    @Param("dueRevenue") BigDecimal dueRevenue,
                    @Param("cashRevenue") BigDecimal cashRevenue,
                    @Param("upiRevenue") BigDecimal upiRevenue,
                    @Param("cardRevenue") BigDecimal cardRevenue);

    @Modifying
    @Transactional
    void deleteByLabIdAndStatDate(Long labId, LocalDate statDate);

    @Query("SELECT MAX(d.statDate) FROM DailyLabCategoryStats d WHERE d.labId = :labId")
    LocalDate findMaxStatDateByLabId(@Param("labId") Long labId);

    // ── Rollup reads — used by the dashboard instead of the slow live join queries ──

    @Query(value =
        "SELECT category, SUM(test_count) AS testCount, " +
        "ROUND(SUM(gross_revenue), 2) AS revenue, ROUND(SUM(discount), 2) AS discount, " +
        "ROUND(SUM(paid_revenue), 2) AS paidRevenue, ROUND(SUM(due_revenue), 2) AS dueRevenue, " +
        "ROUND(SUM(cash_revenue), 2) AS cashRevenue, ROUND(SUM(upi_revenue), 2) AS upiRevenue, " +
        "ROUND(SUM(card_revenue), 2) AS cardRevenue " +
        "FROM daily_lab_category_stats WHERE lab_id = :labId AND stat_date BETWEEN :start AND :end " +
        "GROUP BY category ORDER BY testCount DESC", nativeQuery = true)
    List<VisitTestResultRepository.TestsByCategoryDetailedProjection> rollupByCategoryForLabWithDateRange(
            @Param("labId") Long labId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value =
        "SELECT category, SUM(test_count) AS testCount, " +
        "ROUND(SUM(gross_revenue), 2) AS revenue, ROUND(SUM(discount), 2) AS discount, " +
        "ROUND(SUM(paid_revenue), 2) AS paidRevenue, ROUND(SUM(due_revenue), 2) AS dueRevenue, " +
        "ROUND(SUM(cash_revenue), 2) AS cashRevenue, ROUND(SUM(upi_revenue), 2) AS upiRevenue, " +
        "ROUND(SUM(card_revenue), 2) AS cardRevenue " +
        "FROM daily_lab_category_stats WHERE lab_id = :labId " +
        "GROUP BY category ORDER BY testCount DESC", nativeQuery = true)
    List<VisitTestResultRepository.TestsByCategoryDetailedProjection> rollupByCategoryForLab(
            @Param("labId") Long labId);

    @Query(value =
        "SELECT d.category, SUM(d.test_count) AS testCount, " +
        "ROUND(SUM(d.gross_revenue), 2) AS revenue, ROUND(SUM(d.discount), 2) AS discount, " +
        "ROUND(SUM(d.paid_revenue), 2) AS paidRevenue, ROUND(SUM(d.due_revenue), 2) AS dueRevenue, " +
        "ROUND(SUM(d.cash_revenue), 2) AS cashRevenue, ROUND(SUM(d.upi_revenue), 2) AS upiRevenue, " +
        "ROUND(SUM(d.card_revenue), 2) AS cardRevenue " +
        "FROM daily_lab_category_stats d JOIN labs l ON d.lab_id = l.lab_id " +
        "WHERE l.created_by = :userId AND d.stat_date BETWEEN :start AND :end " +
        "GROUP BY d.category ORDER BY testCount DESC", nativeQuery = true)
    List<VisitTestResultRepository.TestsByCategoryDetailedProjection> rollupByCategoryForUserWithDateRange(
            @Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value =
        "SELECT d.category, SUM(d.test_count) AS testCount, " +
        "ROUND(SUM(d.gross_revenue), 2) AS revenue, ROUND(SUM(d.discount), 2) AS discount, " +
        "ROUND(SUM(d.paid_revenue), 2) AS paidRevenue, ROUND(SUM(d.due_revenue), 2) AS dueRevenue, " +
        "ROUND(SUM(d.cash_revenue), 2) AS cashRevenue, ROUND(SUM(d.upi_revenue), 2) AS upiRevenue, " +
        "ROUND(SUM(d.card_revenue), 2) AS cardRevenue " +
        "FROM daily_lab_category_stats d JOIN labs l ON d.lab_id = l.lab_id " +
        "WHERE l.created_by = :userId " +
        "GROUP BY d.category ORDER BY testCount DESC", nativeQuery = true)
    List<VisitTestResultRepository.TestsByCategoryDetailedProjection> rollupByCategoryForUser(
            @Param("userId") Long userId);

    interface CategorySummaryProjection {
        String getCategory();
        Long getTestCount();
        BigDecimal getGrossRevenue();
        BigDecimal getDiscount();
        BigDecimal getPaidRevenue();
        BigDecimal getDueRevenue();
        BigDecimal getCashRevenue();
        BigDecimal getUpiRevenue();
        BigDecimal getCardRevenue();
    }

    @Query("SELECT d.category AS category, " +
            "SUM(d.testCount) AS testCount, " +
            "SUM(d.grossRevenue) AS grossRevenue, " +
            "SUM(d.discount) AS discount, " +
            "SUM(d.paidRevenue) AS paidRevenue, " +
            "SUM(d.dueRevenue) AS dueRevenue, " +
            "SUM(d.cashRevenue) AS cashRevenue, " +
            "SUM(d.upiRevenue) AS upiRevenue, " +
            "SUM(d.cardRevenue) AS cardRevenue " +
            "FROM DailyLabCategoryStats d WHERE d.labId = :labId AND d.statDate BETWEEN :start AND :end " +
            "GROUP BY d.category")
    List<CategorySummaryProjection> sumRangeByCategoryForLab(@Param("labId") Long labId,
                                                              @Param("start") LocalDate start,
                                                              @Param("end") LocalDate end);
}
