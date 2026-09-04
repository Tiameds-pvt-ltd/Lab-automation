package tiameds.com.tiameds.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rollup row for the per-category tests/revenue breakdown: one row per
 * (lab_id, stat_date, category). Kept in sync by CategoryStatsRollupService,
 * independently of daily_lab_stats — see V41 migration.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@IdClass(DailyLabCategoryStatsId.class)
@Table(name = "daily_lab_category_stats")
public class DailyLabCategoryStats {

    @Id
    @Column(name = "lab_id")
    private Long labId;

    @Id
    @Column(name = "stat_date")
    private LocalDate statDate;

    @Id
    @Column(name = "category")
    private String category;

    @Column(name = "test_count", nullable = false)
    private Long testCount = 0L;

    @Column(name = "gross_revenue", nullable = false)
    private BigDecimal grossRevenue = BigDecimal.ZERO;

    @Column(name = "discount", nullable = false)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "paid_revenue", nullable = false)
    private BigDecimal paidRevenue = BigDecimal.ZERO;

    @Column(name = "due_revenue", nullable = false)
    private BigDecimal dueRevenue = BigDecimal.ZERO;

    @Column(name = "cash_revenue", nullable = false)
    private BigDecimal cashRevenue = BigDecimal.ZERO;

    @Column(name = "upi_revenue", nullable = false)
    private BigDecimal upiRevenue = BigDecimal.ZERO;

    @Column(name = "card_revenue", nullable = false)
    private BigDecimal cardRevenue = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
