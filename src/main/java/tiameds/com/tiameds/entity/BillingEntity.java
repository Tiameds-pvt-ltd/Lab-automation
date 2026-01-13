
package tiameds.com.tiameds.entity;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "billing")
public class BillingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_id")
    private Long id;


    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "payment_date", nullable = false)
    private String paymentDate;

    @Column(name = "discount", nullable = false)
    private BigDecimal discount;

    @Column(name = "gst_rate", nullable = false)
    private BigDecimal gstRate;

    @Column(name = "gst_amount", nullable = false)
    private BigDecimal gstAmount;

    @Column(name = "cgst_amount", nullable = false)
    private BigDecimal cgstAmount;

    @Column(name = "sgst_amount", nullable = false)
    private BigDecimal sgstAmount;

    @Column(name = "igst_amount", nullable = false)
    private BigDecimal igstAmount;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "lab_billing",
            joinColumns = @JoinColumn(name = "billing_id"),
            inverseJoinColumns = @JoinColumn(name = "lab_id"))
    private Set<Lab> labs = new HashSet<>();

    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<TestDiscountEntity> testDiscounts = new HashSet<>();

    @Column(name = "discount_reason")
    private String discountReason;

    // Transaction-related amounts (optional summary)
    @Column(name = "received_amount")
    private BigDecimal receivedAmount;

    @Column(name = "actual_received_amount")
    private BigDecimal actualReceivedAmount;

    @Column(name = "due_amount")
    private BigDecimal dueAmount;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "billing_time", nullable = false)
    private LocalTime billingTime;

    @Column(name = "billing_date")
    private String billingDate;

    @Column(name = "updated_by")
    private String updatedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // New relation for tracking transactions
    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<TransactionEntity> transactions = new HashSet<>();



    @OneToOne(mappedBy = "billing", fetch = FetchType.LAZY)
    private VisitEntity visit;

    @Column(name = "billing_code", unique = true)
    private String billingCode;
//
//    public void setRefundAmount(BigDecimal bigDecimal) {
//        // Method implementation can be added here if needed
//        this.netAmount = bigDecimal;
//    }
}



