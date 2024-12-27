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
    private BigDecimal discount;
    private BigDecimal gstRate;
    private BigDecimal gstAmount;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal netAmount;

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
    }
}