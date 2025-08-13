package tiameds.com.tiameds.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.com.tiameds.entity.PatientEntity;

import java.time.LocalDate;
import java.util.List;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PatientList{
    private Long id;
    private String firstName;
    private String phone;
    private String city;
    private LocalDate dateOfBirth;
    private String gender;
    private String age;


    public PatientList(Long patientId, String firstName, String lastName, String phone, String email, String address, String city, String state, String zip, String bloodGroup, LocalDate dateOfBirth, String gender , String age) {
        this.id = patientId;
        this.firstName = firstName;
        this.phone = phone;
        this.city = city;
        this.dateOfBirth = dateOfBirth;
        this.age = age;
    }

    public PatientList(PatientEntity patientEntity) {
    }
}
