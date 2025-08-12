package tiameds.com.tiameds.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.com.tiameds.entity.VisitEntity;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CancellationDataDTO {

    private String visitCancellationReason;
    private String visitCancellationDate;
    private String visitCancellationTime;

    public CancellationDataDTO(VisitEntity visit) {
        this.visitCancellationReason = visit.getVisitCancellationReason();
        this.visitCancellationDate = String.valueOf(visit.getVisitCancellationDate());
        this.visitCancellationTime = String.valueOf(visit.getVisitCancellationTime());
    }


}
