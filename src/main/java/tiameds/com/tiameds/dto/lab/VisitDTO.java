package tiameds.com.tiameds.dto.lab;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.com.tiameds.entity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VisitDTO {
    private Long visitId;
    private LocalDate visitDate;
    private String visitType; // IN-PATIENT, OUT-PATIENT, EMERGENCY
    private String visitStatus; // ACTIVE, DISCHARGED, CANCELLED
    private String visitDescription;
    private Long doctorId;
    private List<Long> testIds;
    private List<Long> packageIds;
    private List<Long> insuranceIds;
    private BillingDTO billing;
    private String createdBy;
    private String updatedBy;
    //------
    private String visitCancellationReason;
    private LocalDate visitCancellationDate;
    private String visitCancellationBy;
    private LocalDateTime visitCancellationTime;
    @JsonProperty("listofeachtestdiscount")
    private List<TestDiscountDTO> listOfEachTestDiscount;
    @JsonProperty("testResult")
    private List<VisitTestResultResponseDTO> testResult;

    public VisitDTO(VisitEntity visitEntity) {
        this.visitId = visitEntity.getVisitId();
        this.visitDate = visitEntity.getVisitDate();
        this.visitType = visitEntity.getVisitType();
        this.visitStatus = visitEntity.getVisitStatus();
        this.visitDescription = visitEntity.getVisitDescription();
        this.doctorId = (visitEntity.getDoctor() != null) ? visitEntity.getDoctor().getId() : null;
        this.testIds = visitEntity.getTests() != null
                ? visitEntity.getTests().stream().map(Test::getId).collect(Collectors.toList())
                : null;
        this.packageIds = visitEntity.getPackages() != null
                ? visitEntity.getPackages().stream().map(HealthPackage::getId).collect(Collectors.toList())
                : null;
        this.insuranceIds = visitEntity.getInsurance() != null
                ? visitEntity.getInsurance().stream().map(InsuranceEntity::getId).collect(Collectors.toList())
                : null;
        this.billing = new BillingDTO(visitEntity.getBilling());

        Set<TestDiscountEntity> testDiscounts = visitEntity.getBilling().getTestDiscounts();
        if (testDiscounts != null && !testDiscounts.isEmpty()) {
            this.listOfEachTestDiscount = testDiscounts.stream()
                    .map(discount -> {
                        TestDiscountDTO dto = new TestDiscountDTO();
                        dto.setTestId(discount.getTestId());
                        dto.setDiscountAmount(discount.getDiscountAmount());
                        dto.setDiscountPercent(discount.getDiscountPercent());
                        dto.setFinalPrice(discount.getFinalPrice());
                        dto.setCreatedBy(discount.getCreatedBy());
                        dto.setUpdatedBy(discount.getUpdatedBy());
//                        dto.setBillingId(discount.getBilling().getId());
                        return dto;
                    })
                    .collect(Collectors.toList());
        }

        // new fields for cancellation
        this.visitCancellationReason = visitEntity.getVisitCancellationReason();
        this.visitCancellationDate = visitEntity.getVisitCancellationDate();
        this.visitCancellationBy = visitEntity.getVisitCancellationBy();
        this.visitCancellationTime = visitEntity.getVisitCancellationTime();
        // If visitCancellationReason is null, set it to an empty string
//        this.visitTime = visitEntity.getVisitTime();
        this.createdBy = visitEntity.getCreatedBy();
        this.updatedBy = visitEntity.getUpdatedBy();

        // Populate test results
        if (visitEntity.getTestResults() != null && !visitEntity.getTestResults().isEmpty()) {
            this.testResult = visitEntity.getTestResults().stream()
                    .map(VisitTestResultResponseDTO::new)
                    .collect(Collectors.toList());
        } else {
            this.testResult = List.of();
        }

    }

}
