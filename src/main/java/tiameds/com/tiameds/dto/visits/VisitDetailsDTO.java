package tiameds.com.tiameds.dto.visits;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.com.tiameds.dto.lab.TestDiscountDTO;
import tiameds.com.tiameds.dto.lab.VisitTestResultResponseDTO;
import tiameds.com.tiameds.entity.HealthPackage;
import tiameds.com.tiameds.entity.Test;
import tiameds.com.tiameds.entity.VisitEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VisitDetailsDTO {
    private Long visitId;
    private String visitCode;
    private LocalDate visitDate;
    private String visitType;
    private String visitStatus;
    private String visitDescription;
    private Long doctorId;
    private String doctorName;
    private List<Long> testIds;
    private List<Long> packageIds;
//    private List<Long> insuranceIds;
    private BillDTO billing;
    private String createdBy;
    private String updatedBy;
    private String visitCancellationReason;
    private LocalDate visitCancellationDate;
    private String visitCancellationBy;
    private Instant visitCancellationTime;
    @JsonProperty("listofeachtestdiscount")
    private List<TestDiscountDTO> listOfEachTestDiscount;
    private List <VisitTestResultResponseDTO> testResult;
    public VisitDetailsDTO(VisitEntity visitEntity) {
        this.visitId = visitEntity.getVisitId();
        this.visitCode = visitEntity.getVisitCode();
        this.visitDate = visitEntity.getVisitDate();
        this.visitType = visitEntity.getVisitType();
        this.visitStatus = visitEntity.getVisitStatus();
        this.visitDescription = visitEntity.getVisitDescription();
        this.doctorId = (visitEntity.getDoctor() != null) ? visitEntity.getDoctor().getId() : null;
        this.doctorName = (visitEntity.getDoctor() != null) ? visitEntity.getDoctor().getName() : null;

        this.testIds = visitEntity.getTests() != null
                ? visitEntity.getTests().stream().map(Test::getId).collect(Collectors.toList())
                : Collections.emptyList();

        this.packageIds = visitEntity.getPackages() != null
                ? visitEntity.getPackages().stream().map(HealthPackage::getId).collect(Collectors.toList())
                : Collections.emptyList();
        if (visitEntity.getBilling() != null) {
            this.billing = new BillDTO(visitEntity.getBilling());

            // Handle test discounts
            if (visitEntity.getBilling().getTestDiscounts() != null) {
                this.listOfEachTestDiscount = visitEntity.getBilling().getTestDiscounts().stream()
                        .map(discount -> {
                            TestDiscountDTO dto = new TestDiscountDTO();
                            dto.setTestId(discount.getTestId());
                            dto.setDiscountAmount(discount.getDiscountAmount());
                            dto.setDiscountPercent(discount.getDiscountPercent());
                            dto.setFinalPrice(discount.getFinalPrice());
                            dto.setCreatedBy(discount.getCreatedBy());
                            dto.setUpdatedBy(discount.getUpdatedBy());
                            return dto;
                        })
                        .collect(Collectors.toList());
            }
        } else {
            this.billing = null;
            this.listOfEachTestDiscount = Collections.emptyList();
        }

        // Handle test results
        if (visitEntity.getTestResults() != null) {
            this.testResult = visitEntity.getTestResults().stream()
                    .map(result -> new VisitTestResultResponseDTO(result))
                    .collect(Collectors.toList());
        } else {
            this.testResult = Collections.emptyList();
        }

        // Cancellation fields
        this.visitCancellationReason = visitEntity.getVisitCancellationReason();
        this.visitCancellationDate = visitEntity.getVisitCancellationDate();
        this.visitCancellationBy = visitEntity.getVisitCancellationBy();
        this.visitCancellationTime = visitEntity.getVisitCancellationTime();
        this.createdBy = visitEntity.getCreatedBy();
        this.updatedBy = visitEntity.getUpdatedBy();
    }
}
