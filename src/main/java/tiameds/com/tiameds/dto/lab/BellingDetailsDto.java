package tiameds.com.tiameds.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BellingDetailsDto {
    private Long billingId;
    private BigDecimal totalAmount;
    private String paymentStatus; // PAID, UNPAID, PARTIAL
    private String paymentMethod; // CASH, CARD, ONLINE
    private String paymentDate;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal netAmount = BigDecimal.ZERO;
    private String discountReason;
    private BigDecimal discountPercentage = BigDecimal.ZERO;
}
