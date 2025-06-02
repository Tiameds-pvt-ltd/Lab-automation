package tiameds.com.tiameds.dto.lab;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.com.tiameds.entity.BillingEntity;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BillingDTO {
    private Long billingId;
    private BigDecimal totalAmount;
    private String paymentStatus; // PAID, UNPAID, PARTIAL
    private String paymentMethod; // CASH, CARD, ONLINE
    private String paymentDate; // Date of payment
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal gstRate = BigDecimal.ZERO;
    private BigDecimal gstAmount = BigDecimal.ZERO;
    private BigDecimal cgstAmount = BigDecimal.ZERO;
    private BigDecimal sgstAmount = BigDecimal.ZERO;
    private BigDecimal igstAmount = BigDecimal.ZERO;
    private BigDecimal netAmount = BigDecimal.ZERO;
    private String discountReason;
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    public BillingDTO(BillingEntity billing) {
        this.billingId = billing.getId();
        this.totalAmount = billing.getTotalAmount();
        this.paymentStatus = billing.getPaymentStatus();
        this.paymentMethod = billing.getPaymentMethod();
        this.paymentDate = billing.getPaymentDate();
        this.discount = billing.getDiscount();
        this.gstRate = billing.getGstRate();
        this.gstAmount = billing.getGstAmount();
        this.cgstAmount = billing.getCgstAmount();
        this.sgstAmount = billing.getSgstAmount();
        this.igstAmount = billing.getIgstAmount();
        this.netAmount = billing.getNetAmount();
        this.discountReason = billing.getDiscountReason();

    }

    public BillingDTO(Long id, BigDecimal totalAmount, String paymentStatus, String paymentMethod, String s, BigDecimal discount, BigDecimal gstRate, BigDecimal gstAmount, BigDecimal cgstAmount, BigDecimal sgstAmount, BigDecimal igstAmount, BigDecimal netAmount, String discountReason) {
    }
}