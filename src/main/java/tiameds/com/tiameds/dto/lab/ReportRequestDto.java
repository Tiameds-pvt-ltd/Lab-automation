package tiameds.com.tiameds.dto.lab;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReportRequestDto {

    @JsonProperty("testData")
    private List<ReportDto> testData;

    @JsonProperty("testResult")
    private TestResultDto testResult;
}
