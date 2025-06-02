package tiameds.com.tiameds.dto.lab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TestDiscountDTO {
    @JsonProperty("id")
    private Long testId;
    private BigDecimal discountAmount;
    private BigDecimal discountPercent;
    private BigDecimal finalPrice;
    private String createdBy;
    private String updatedBy;


    public TestDiscountDTO(Long id, Long testId, BigDecimal finalPrice, BigDecimal discountAmount, BigDecimal discountPercent) {
    }


}