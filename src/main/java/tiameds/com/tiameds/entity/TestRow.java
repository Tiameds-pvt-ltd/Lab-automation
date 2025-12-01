package tiameds.com.tiameds.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestRow {

    private String testParameter;
    private String normalRange;
    private String enteredValue;
    private String unit;
    private String referenceAgeRange;
}

