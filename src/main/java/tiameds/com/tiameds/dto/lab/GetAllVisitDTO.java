package tiameds.com.tiameds.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GetAllVisitDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String zip;
    private String bloodGroup;
    private LocalDate dateOfBirth;
    private VisitDTO visit;

    // Getters and Setters

    public static class VisitDTO {
        private Long visitId;
        private LocalDate visitDate;
        private String visitType;
        private String visitStatus;
        private String visitDescription;
        private Long doctorId;
        private List<Long> testIds;
        private List<Long> packageIds;
        private List<Long> insuranceIds;
        private BillingDTO billing;

        // Getters and Setters
    }

    public static class BillingDTO {
        private Long billingId;
        private BigDecimal totalAmount;
        private String paymentStatus;
        private String paymentMethod;
        private LocalDate paymentDate;
        private BigDecimal discount;
        private BigDecimal gstRate;
        private BigDecimal gstAmount;
        private BigDecimal cgstAmount;
        private BigDecimal sgstAmount;
        private BigDecimal igstAmount;
        private BigDecimal netAmount;

        // Getters and Setters
    }
}
