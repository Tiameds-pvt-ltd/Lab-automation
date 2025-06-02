package tiameds.com.tiameds.dto.lab;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorDTO {
    private long id;
    private String name;
    private String email;
    private String speciality;
    private String qualification;
    private String hospitalAffiliation;
    private String licenseNumber;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String country;
    private String CreatedBy;
    private String UpdatedBy;
//    private Set<Long> labIds; // Lab IDs related to the doctor
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}