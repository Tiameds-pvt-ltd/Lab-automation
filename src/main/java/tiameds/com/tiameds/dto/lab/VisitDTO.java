//package tiameds.com.tiameds.dto.lab;
//
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.annotation.JsonInclude;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.springframework.data.annotation.CreatedBy;
//import tiameds.com.tiameds.entity.HealthPackage;
//import tiameds.com.tiameds.entity.InsuranceEntity;
//import tiameds.com.tiameds.entity.Test;
//import tiameds.com.tiameds.entity.VisitEntity;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//@Getter
//@Setter
//@AllArgsConstructor
//@NoArgsConstructor
//@JsonInclude(JsonInclude.Include.NON_NULL)
//@JsonIgnoreProperties(ignoreUnknown = true)
//public class VisitDTO {
//    private long visitId;
//    private LocalDate visitDate;
//    private String visitType; // IN-PATIENT, OUT-PATIENT, EMERGENCY
//    private String visitStatus; // ACTIVE, DISCHARGED, CANCELLED
//    private String visitDescription;
//    private Long doctorId;
////    private Optional<Long> doctorId;
//    private List<Long> testIds; // Test IDs associated with the visit
//    private List<Long> packageIds; // Health package IDs
//    private List<Long> insuranceIds; // Insurance IDs
//    private BillingDTO billing;
//    private List<TestDiscountDTO> listofeachtestdiscoun;
//
//    public VisitDTO(VisitEntity visitEntity) {
//        this.visitId = visitEntity.getVisitId();
//        this.visitDate = visitEntity.getVisitDate();
//        this.visitType = visitEntity.getVisitType();
//        this.visitStatus = visitEntity.getVisitStatus();
//        this.visitDescription = visitEntity.getVisitDescription();
////        this.doctorId = visitEntity.getDoctor().getId();
//        this.doctorId = (visitEntity.getDoctor()!= null) ? visitEntity.getDoctor().getId() : null;
//
//        this.testIds = visitEntity.getTests().stream().map(Test::getId).collect(Collectors.toList());
//        this.packageIds = visitEntity.getPackages().stream().map(HealthPackage::getId).collect(Collectors.toList());
//        this.insuranceIds = visitEntity.getInsurance().stream().map(InsuranceEntity::getId).collect(Collectors.toList());
//        this.billing = new BillingDTO(visitEntity.getBilling());
//
//
//    }
//
//
//}


package tiameds.com.tiameds.dto.lab;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.com.tiameds.entity.*;

import java.time.LocalDate;
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

    @JsonProperty("listofeachtestdiscount")
    private List<TestDiscountDTO> listOfEachTestDiscount;


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
    }

}
