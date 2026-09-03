package tiameds.com.tiameds.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class DailyLabStatsId implements Serializable {

    private Long labId;
    private LocalDate statDate;

    public DailyLabStatsId() {
    }

    public DailyLabStatsId(Long labId, LocalDate statDate) {
        this.labId = labId;
        this.statDate = statDate;
    }

    public Long getLabId() {
        return labId;
    }

    public void setLabId(Long labId) {
        this.labId = labId;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyLabStatsId)) return false;
        DailyLabStatsId that = (DailyLabStatsId) o;
        return Objects.equals(labId, that.labId) && Objects.equals(statDate, that.statDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labId, statDate);
    }
}
