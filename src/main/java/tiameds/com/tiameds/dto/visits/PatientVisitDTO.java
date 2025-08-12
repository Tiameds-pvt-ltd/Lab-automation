package tiameds.com.tiameds.dto.visits;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.com.tiameds.dto.lab.VisitDTO;
import tiameds.com.tiameds.entity.PatientEntity;
import tiameds.com.tiameds.entity.VisitEntity;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PatientVisitDTO {
    private Long id;
    private String firstName;
    private String phone;
    private String city;
    private LocalDate dateOfBirth;
    private String age;
    private String gender;
    private VisitDetailsDTO visit;
    private String createdBy;
    private String updatedBy;


    public PatientVisitDTO(PatientEntity patient, VisitEntity visit) {

    }
}
