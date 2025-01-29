package tiameds.com.tiameds.dto.lab;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.com.tiameds.entity.PatientEntity;
import tiameds.com.tiameds.entity.VisitEntity;

import java.time.LocalDate;
import java.util.Comparator;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PatientDTO {
    private long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String zip;
    private String bloodGroup;
    private LocalDate dateOfBirth;
    private String gender;
    private VisitDTO visit; // Single visit for simplicity in this context

    public PatientDTO(PatientEntity patient) {

        this.id = patient.getId();
        this.firstName = patient.getFirstName();
        this.lastName = patient.getLastName();
        this.email = patient.getEmail();
        this.phone = patient.getPhone();
        this.address = patient.getAddress();
        this.city = patient.getCity();
        this.state = patient.getState();
        this.zip = patient.getZip();
        this.bloodGroup = patient.getBloodGroup();
        this.dateOfBirth = patient.getDateOfBirth();
        this.gender = patient.getGender();

        //get latest visit     on the basis of visiting id
        VisitEntity latestVisit = patient.getVisits().stream().max(Comparator.comparing(VisitEntity::getVisitId)).orElse(null);

        if (latestVisit != null) {
            this.visit = new VisitDTO(latestVisit);
        }

    }


}