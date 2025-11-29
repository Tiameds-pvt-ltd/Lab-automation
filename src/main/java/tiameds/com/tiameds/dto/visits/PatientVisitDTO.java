package tiameds.com.tiameds.dto.visits;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.com.tiameds.entity.PatientEntity;
import tiameds.com.tiameds.entity.VisitEntity;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientVisitDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String patientCode;
    private String phone;
    private String city;
    private LocalDate dateOfBirth;
    private String age;
    private String gender;
    private VisitDetailsDTO visit;
    private String createdBy;
    private String updatedBy;

    public PatientVisitDTO(PatientEntity patient, VisitEntity visit) {
        this.id = patient.getPatientId();
        this.firstName = patient.getFirstName();
        this.lastName = patient.getLastName();
        this.patientCode = patient.getPatientCode();
        this.phone = patient.getPhone();
        this.city = patient.getCity();
        this.dateOfBirth = patient.getDateOfBirth();
        this.age = patient.getAge();
        this.gender = patient.getGender();
        this.visit = visit != null ? new VisitDetailsDTO(visit) : null;
        this.createdBy = visit != null ? visit.getCreatedBy() : null;
        this.updatedBy = visit != null ? visit.getUpdatedBy() : null;
    }
}
