package tiameds.com.tiameds.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.IdClass;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rollup row for the super-admin dashboard: one row per (lab_id, stat_date),
 * kept in sync incrementally by DashboardRollupService whenever billing/visit/
 * report data for that lab+day changes. See daily_lab_stats migration (V17).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@IdClass(DailyLabStatsId.class)
@Table(name = "daily_lab_stats")
public class DailyLabStats {

    @Id
    @Column(name = "lab_id")
    private Long labId;

    @Id
    @Column(name = "stat_date")
    private LocalDate statDate;

    @Column(name = "test_count", nullable = false)
    private Long testCount = 0L;

    @Column(name = "reports_generated", nullable = false)
    private Long reportsGenerated = 0L;

    @Column(name = "pending_samples", nullable = false)
    private Long pendingSamples = 0L;

    @Column(name = "patient_count", nullable = false)
    private Long patientCount = 0L;

    @Column(name = "paid_revenue", nullable = false)
    private BigDecimal paidRevenue = BigDecimal.ZERO;

    @Column(name = "due_revenue", nullable = false)
    private BigDecimal dueRevenue = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
