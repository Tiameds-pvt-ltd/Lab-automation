package tiameds.com.tiameds.dto.lab;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PatientVisitSampleDto {
    private Long visitId;
    private List<String> sampleNames;
}
