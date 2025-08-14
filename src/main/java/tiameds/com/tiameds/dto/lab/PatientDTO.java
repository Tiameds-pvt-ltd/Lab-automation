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
    private Long id;
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
    private String age; // This can be calculated based on dateOfBirth
    private String gender;
    private VisitDTO visit;
    private String createdBy;
    private String updatedBy;


    public PatientDTO(PatientEntity patient) {
        this.id = patient.getPatientId();
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
        this.age = patient.getAge();
        this.createdBy = patient.getCreatedBy();
        this.updatedBy = patient.getUpdatedBy();

        // Get latest visit
        if (patient.getVisits() != null && !patient.getVisits().isEmpty()) {
            VisitEntity latestVisit = patient.getVisits().stream()
                    .max(Comparator.comparing(VisitEntity::getVisitId))
                    .orElse(null);
            if (latestVisit != null) {
                this.visit = new VisitDTO(latestVisit);
            }
        }
    }
}