package tiameds.com.tiameds.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PatientDetailsDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String city;
    private LocalDate dateOfBirth;
    private String gender;
    private VisitDetailDto visitDetailDto;
}
