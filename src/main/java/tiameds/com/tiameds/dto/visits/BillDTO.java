package tiameds.com.tiameds.dto.visits;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BillDTO {
    private Long billingId;
    private String billingCode;
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

    private Set<TransactionDTO> transactions;


    public BillDTO(BillingEntity billing) {
        this.billingId = billing.getId();
        this.billingCode = billing.getBillingCode();
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
        this.transactions = billing.getTransactions() != null && !billing.getTransactions().isEmpty()
                ? billing.getTransactions().stream()
                        .map(TransactionDTO::new)
                        .collect(Collectors.toSet())
                : Collections.emptySet();
    }
}
