package tiameds.com.tiameds.dto.visits;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.com.tiameds.dto.lab.TransactionDTO;
import tiameds.com.tiameds.entity.BillingEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BillDtoDue {
    private Long billingId;
    private BigDecimal totalAmount;
    private String paymentStatus; // PAID, UNPAID, PARTIAL
    private String paymentMethod; // CASH, CARD, ONLINE
    private String paymentDate;
    private BigDecimal discount;
    private BigDecimal netAmount;
    private String discountReason;
    @JsonProperty("received_amount")
    private BigDecimal receivedAmount;
    
    @JsonProperty("actual_received_amount")
    private BigDecimal actualReceivedAmount;
    
    @JsonProperty("due_amount")
    private BigDecimal dueAmount;
    
    @JsonProperty("refund_amount")
    private BigDecimal refundAmount;
    
    private String createdBy;
    private LocalTime billingTime;
    private String billingDate;
    private String updatedBy;
    private Instant createdAt;
    private Instant updatedAt;
    @JsonProperty("transaction")  // This matches your JSON structure
    private TransactionDTO transaction;
    @JsonIgnore
    private Set<TransactionDTO> transactions;
    // Add this helper method
    public TransactionDTO getTransactionData() {
        return this.transaction;
    }
    public BillDtoDue(BillingEntity billing) {
        this.billingId = billing.getId();
        this.totalAmount = billing.getTotalAmount();
        this.paymentStatus = billing.getPaymentStatus();
        this.paymentMethod = billing.getPaymentMethod();
        this.paymentDate = billing.getPaymentDate();
        this.discount = billing.getDiscount();
        this.netAmount = billing.getNetAmount();
        this.discountReason = billing.getDiscountReason();
        this.receivedAmount = billing.getReceivedAmount();
        this.actualReceivedAmount = billing.getActualReceivedAmount();
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
        // Initialize transactions if they exist
        if (billing.getTransactions() != null && !billing.getTransactions().isEmpty()) {
            this.transactions = billing.getTransactions().stream()
                    .map(transaction -> {
                        TransactionDTO dto = new TransactionDTO();
                        dto.setId(transaction.getId());
                        dto.setBillingId(billing.getId());
                        dto.setPaymentMethod(transaction.getPaymentMethod());
                        dto.setUpiId(transaction.getUpiId());
                        dto.setUpiAmount(transaction.getUpiAmount());
                        dto.setCardAmount(transaction.getCardAmount());
                        dto.setCashAmount(transaction.getCashAmount());
                        dto.setReceivedAmount(transaction.getReceivedAmount());
                        dto.setRefundAmount(transaction.getRefundAmount());
                        dto.setDueAmount(transaction.getDueAmount());
                        dto.setPaymentDate(transaction.getPaymentDate());
                        dto.setRemarks(transaction.getRemarks());
                        dto.setCreatedAt(transaction.getCreatedAt());
                        return dto;
                    })
                    .collect(Collectors.toSet());
        } else {
            this.transactions = Collections.emptySet();
        }
    }
}
