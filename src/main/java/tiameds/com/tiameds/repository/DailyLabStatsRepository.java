package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.DailyLabStats;
import tiameds.com.tiameds.entity.DailyLabStatsId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DailyLabStatsRepository extends JpaRepository<DailyLabStats, DailyLabStatsId> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO daily_lab_stats " +
            "(lab_id, stat_date, test_count, reports_generated, pending_samples, patient_count, paid_revenue, due_revenue, updated_at) " +
            "VALUES (:labId, :statDate, :testCount, :reportsGenerated, :pendingSamples, :patientCount, :paidRevenue, :dueRevenue, now()) " +
            "ON CONFLICT (lab_id, stat_date) DO UPDATE SET " +
            "test_count = EXCLUDED.test_count, " +
            "reports_generated = EXCLUDED.reports_generated, " +
            "pending_samples = EXCLUDED.pending_samples, " +
            "patient_count = EXCLUDED.patient_count, " +
            "paid_revenue = EXCLUDED.paid_revenue, " +
            "due_revenue = EXCLUDED.due_revenue, " +
            "updated_at = now()",
            nativeQuery = true)
    void upsertRow(@Param("labId") Long labId,
                    @Param("statDate") LocalDate statDate,
                    @Param("testCount") long testCount,
                    @Param("reportsGenerated") long reportsGenerated,
                    @Param("pendingSamples") long pendingSamples,
                    @Param("patientCount") long patientCount,
                    @Param("paidRevenue") BigDecimal paidRevenue,
                    @Param("dueRevenue") BigDecimal dueRevenue);

    List<DailyLabStats> findByLabIdAndStatDateBetween(Long labId, LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(d.paidRevenue), 0) FROM DailyLabStats d WHERE d.labId = :labId AND d.statDate BETWEEN :start AND :end")
    BigDecimal sumPaidRevenueByLabIdAndDateRange(@Param("labId") Long labId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
