package tiameds.com.tiameds.dto.lab;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VisitDetailDto {
    private Long visitId;
    private LocalDate visitDate;
    private String visitType; // IN-PATIENT, OUT-PATIENT, EMERGENCY
    private String visitStatus; // ACTIVE, DISCHARGED, CANCELLED
    private Long doctorId;
    private List<Long> testIds;
    private List<Long> packageIds;

    @JsonProperty("listofeachtestdiscount")
    private List<TestDiscountDTO> listOfEachTestDiscount;
    private BellingDetailsDto bellingDetailsDto;


}

