//package tiameds.com.tiameds.dto.lab;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import tiameds.com.tiameds.entity.BillingEntity;
//
//import java.math.BigDecimal;
//
//@Getter
//@Setter
//@AllArgsConstructor
//@NoArgsConstructor
//public class BillingDTO {
//    private Long billingId;
//    private BigDecimal totalAmount;
//    private String paymentStatus; // PAID, UNPAID, PARTIAL
//    private String paymentMethod; // CASH, CARD, ONLINE
//    private String paymentDate; // Date of payment
//    private BigDecimal discount = BigDecimal.ZERO;
//    private BigDecimal gstRate = BigDecimal.ZERO;
//    private BigDecimal gstAmount = BigDecimal.ZERO;
//    private BigDecimal cgstAmount = BigDecimal.ZERO;
//    private BigDecimal sgstAmount = BigDecimal.ZERO;
//    private BigDecimal igstAmount = BigDecimal.ZERO;
//    private BigDecimal netAmount = BigDecimal.ZERO;
//    private String discountReason;
//    private BigDecimal discountPercentage = BigDecimal.ZERO;
//
//    // new field
//    @JsonProperty("upi_id")
//    private String upiId;
//
//    @JsonProperty("received_amount")
//    private BigDecimal receivedAmount;
//
//    @JsonProperty("refund_amount")
//    private BigDecimal refundAmount;
//
//    @JsonProperty("upi_amount")
//    private BigDecimal upiAmount;
//
//    @JsonProperty("card_amount")
//    private BigDecimal cardAmount;
//
//    @JsonProperty("cash_amount")
//    private BigDecimal cashAmount;
//
//    @JsonProperty("due_amount")
//    private BigDecimal dueAmount;
//
//    public BillingDTO(BillingEntity billing) {
//        this.billingId = billing.getId();
//        this.totalAmount = billing.getTotalAmount();
//        this.paymentStatus = billing.getPaymentStatus();
//        this.paymentMethod = billing.getPaymentMethod();
//        this.paymentDate = billing.getPaymentDate();
//        this.discount = billing.getDiscount();
//        this.gstRate = billing.getGstRate();
//        this.gstAmount = billing.getGstAmount();
//        this.cgstAmount = billing.getCgstAmount();
//        this.sgstAmount = billing.getSgstAmount();
//        this.igstAmount = billing.getIgstAmount();
//        this.netAmount = billing.getNetAmount();
//        this.discountReason = billing.getDiscountReason();
//        //----
//        this.upiId = billing.getUpiId();
//        this.receivedAmount = billing.getReceivedAmount();
//        this.refundAmount = billing.getRefundAmount();
//        this.upiAmount = billing.getUpiAmount();
//        this.cardAmount = billing.getCardAmount();
//        this.cashAmount = billing.getCashAmount();
//        this.dueAmount = billing.getDueAmount();
//
//    }
//
//
//    public BillingDTO(Long id, BigDecimal totalAmount, String paymentStatus, String paymentMethod, String s, BigDecimal discount, BigDecimal gstRate, BigDecimal gstAmount, BigDecimal cgstAmount, BigDecimal sgstAmount, BigDecimal igstAmount, BigDecimal netAmount, String discountReason) {
//    }
//}
//
//
//
//
//





package tiameds.com.tiameds.dto.lab;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.com.tiameds.entity.BillingEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BillingDTO {

    private Long billingId;
    private BigDecimal totalAmount;
    private String paymentStatus; // PAID, UNPAID, PARTIAL
    private String paymentMethod; // CASH, CARD, ONLINE
    private String paymentDate;
    private BigDecimal discount;
    private BigDecimal gstRate;
    private BigDecimal gstAmount;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal netAmount;
    private String discountReason;

    @JsonProperty("received_amount")
    private BigDecimal receivedAmount;

    @JsonProperty("due_amount")
    private BigDecimal dueAmount;
    
    @JsonProperty("refund_amount")
    private BigDecimal refundAmount;
    
    private String createdBy;
    private LocalTime billingTime;
    private String billingDate;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<TransactionDTO> transactions;

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
        this.receivedAmount = billing.getReceivedAmount();
        this.dueAmount = billing.getDueAmount();
        
        // Calculate total refund amount from existing transactions
        this.refundAmount = billing.getTransactions() != null ? 
            billing.getTransactions().stream()
                .map(t -> t.getRefundAmount() != null ? t.getRefundAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : 
            BigDecimal.ZERO;
        
        this.createdBy = billing.getCreatedBy();
        this.billingTime = billing.getBillingTime();
        this.billingDate = billing.getBillingDate();
        this.updatedBy = billing.getUpdatedBy();
        this.createdAt = billing.getCreatedAt();
        this.updatedAt = billing.getUpdatedAt();

        if (billing.getTransactions() != null) {
            this.transactions = billing.getTransactions().stream()
                    .map(TransactionDTO::new)
                    .collect(Collectors.toSet());
        } else {
            this.transactions = Set.of();
        }
    }
}
