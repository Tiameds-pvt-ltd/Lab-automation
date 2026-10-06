package tiameds.com.tiameds.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DescriptionMasterDTO {

    private Integer descriptionId;
    private String descriptionName;
    private String resultType;
    private String options;
    private Integer parameterId;
}
