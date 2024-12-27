package tiameds.com.tiameds.dto.lab;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.com.tiameds.entity.HealthPackage;
import tiameds.com.tiameds.entity.InsuranceEntity;
import tiameds.com.tiameds.entity.Test;
import tiameds.com.tiameds.entity.VisitEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VisitDTO {
    private long visitId;
    private LocalDate visitDate;
    private String visitType; // IN-PATIENT, OUT-PATIENT, EMERGENCY
    private String visitStatus; // ACTIVE, DISCHARGED, CANCELLED
    private String visitDescription;
    private Long doctorId;
    private List<Long> testIds; // Test IDs associated with the visit
    private List<Long> packageIds; // Health package IDs
    private List<Long> insuranceIds; // Insurance IDs
    private BillingDTO billing;

    public VisitDTO(VisitEntity visitEntity) {
        this.visitId = visitEntity.getVisitId();
        this.visitDate = visitEntity.getVisitDate();
        this.visitType = visitEntity.getVisitType();
        this.visitStatus = visitEntity.getVisitStatus();
        this.visitDescription = visitEntity.getVisitDescription();
        this.doctorId = visitEntity.getDoctor().getId();
        this.testIds = visitEntity.getTests().stream().map(Test::getId).collect(Collectors.toList());
        this.packageIds = visitEntity.getPackages().stream().map(HealthPackage::getId).collect(Collectors.toList());
        this.insuranceIds = visitEntity.getInsurance().stream().map(InsuranceEntity::getId).collect(Collectors.toList());
        this.billing = new BillingDTO(visitEntity.getBilling());
    }


}