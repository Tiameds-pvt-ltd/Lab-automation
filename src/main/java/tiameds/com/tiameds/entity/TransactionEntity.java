package tiameds.com.tiameds.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "billing_transaction")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id", nullable = false)
    @JsonBackReference
    private BillingEntity billing;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // CASH, CARD, UPI

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "upi_amount")
    private BigDecimal upiAmount;

    @Column(name = "card_amount")
    private BigDecimal cardAmount;

    @Column(name = "cash_amount")
    private BigDecimal cashAmount;

    @Column(name = "received_amount")
    private BigDecimal receivedAmount;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "due_amount")
    private BigDecimal dueAmount;

    @Column(name = "payment_date", nullable = false)
    private String paymentDate;

    @Column(name = "remarks")
    private String remarks;

    @Column(name ="created_by")
    private String createdBy;


    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @Column(name = "transaction_code", unique = true)
    private String transactionCode;

}
