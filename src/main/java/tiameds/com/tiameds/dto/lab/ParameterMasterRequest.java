package tiameds.com.tiameds.dto.lab;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParameterMasterRequest {
    private String parameterName;
    private Boolean isActive;
}
