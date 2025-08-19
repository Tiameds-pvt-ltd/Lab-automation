package tiameds.com.tiameds.dto.lab;


import lombok.*;
import tiameds.com.tiameds.entity.VisitTestResult;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VisitTestResultResponseDTO {
    private Long id;
    private Long testId;
    private Boolean isFilled;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public VisitTestResultResponseDTO(VisitTestResult visitTestResult) {
        this.id = visitTestResult.getId();
        this.testId = visitTestResult.getTest().getId();
        this.isFilled = visitTestResult.getIsFilled();
        this.createdBy = visitTestResult.getCreatedBy();
        this.updatedBy = visitTestResult.getUpdatedBy();
        this.createdAt = visitTestResult.getCreatedAt();
        this.updatedAt = visitTestResult.getUpdatedAt();


    }

    public VisitTestResultResponseDTO(Long id, long id1, String name, Boolean isFilled, LocalDateTime createdAt) {
    }
}