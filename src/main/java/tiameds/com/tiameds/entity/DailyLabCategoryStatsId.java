package tiameds.com.tiameds.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class DailyLabCategoryStatsId implements Serializable {

    private Long labId;
    private LocalDate statDate;
    private String category;

    public DailyLabCategoryStatsId() {
    }

    public DailyLabCategoryStatsId(Long labId, LocalDate statDate, String category) {
        this.labId = labId;
        this.statDate = statDate;
        this.category = category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyLabCategoryStatsId)) return false;
        DailyLabCategoryStatsId that = (DailyLabCategoryStatsId) o;
        return Objects.equals(labId, that.labId)
                && Objects.equals(statDate, that.statDate)
                && Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labId, statDate, category);
    }
}
