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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BillDTO {
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
    @JsonProperty("due_amount")
    private BigDecimal dueAmount;
    private String createdBy;
    private LocalTime billingTime;
    private String billingDate;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Set<TransactionDTO> transactions;


    public BillDTO(BillingEntity billing) {
        this.billingId = billing.getId();
        this.totalAmount = billing.getTotalAmount();
        this.paymentStatus = billing.getPaymentStatus();
        this.paymentMethod = billing.getPaymentMethod();
        this.paymentDate = billing.getPaymentDate();
        this.discount = billing.getDiscount();
        this.netAmount = billing.getNetAmount();
        this.discountReason = billing.getDiscountReason();
        this.receivedAmount = billing.getReceivedAmount();
        this.dueAmount = billing.getDueAmount();
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
                        dto.setCreatedBy(transaction.getCreatedBy());
                        return dto;
                    })
                    .collect(Collectors.toSet());
        } else {
            this.transactions = Collections.emptySet();
        }
    }
}
