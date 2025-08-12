package tiameds.com.tiameds.dto.visits;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.com.tiameds.dto.lab.VisitDetailDto;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VisitDetailsResponseDTO {
    private PatientVisitDTO patientVisit;
    private VisitDetailsDTO visitDetails;
    private VisitDetailsDTO visitDetail;
}
