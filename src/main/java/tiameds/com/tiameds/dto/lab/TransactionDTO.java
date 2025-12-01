package tiameds.com.tiameds.dto.lab;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tiameds.com.tiameds.entity.TransactionEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDTO {

    private Long id;

    @JsonProperty("billing_id")
    private Long billingId;
//    private String billingId;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("upi_id")
    private String upiId;

    @JsonProperty("upi_amount")
    private BigDecimal upiAmount;

    @JsonProperty("card_amount")
    private BigDecimal cardAmount;

    @JsonProperty("cash_amount")
    private BigDecimal cashAmount;

    @JsonProperty("received_amount")
    private BigDecimal receivedAmount;

    @JsonProperty("refund_amount")
    private BigDecimal refundAmount;

    @JsonProperty("due_amount")
    private BigDecimal dueAmount;

    @JsonProperty("payment_date")
    private String paymentDate;

    @JsonProperty("remarks")
    private String remarks;

    @Column(name = "created_by")
    private String createdBy;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    private String transactionCode;

    public TransactionDTO(TransactionEntity transaction) {
        this.id = transaction.getId();
        this.billingId = transaction.getBilling() != null ? transaction.getBilling().getId() : null;
        this.paymentMethod = transaction.getPaymentMethod();
        this.upiId = transaction.getUpiId();
        this.upiAmount = transaction.getUpiAmount();
        this.cardAmount = transaction.getCardAmount();
        this.cashAmount = transaction.getCashAmount();
        this.receivedAmount = transaction.getReceivedAmount();
        this.refundAmount = transaction.getRefundAmount();
        this.dueAmount = transaction.getDueAmount();
        this.paymentDate = transaction.getPaymentDate();
        this.remarks = transaction.getRemarks();
        this.createdAt = transaction.getCreatedAt();
        this.createdBy = transaction.getCreatedBy();
        this.transactionCode = transaction.getTransactionCode();
    }
}
