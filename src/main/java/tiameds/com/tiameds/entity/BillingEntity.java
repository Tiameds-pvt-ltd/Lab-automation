package tiameds.com.tiameds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private BigDecimal totalAmount; // Total amount before taxes and discount

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus; // PAID, UNPAID, PARTIAL

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // CASH, CARD, ONLINE

    @Column(name = "payment_date", nullable = false)
    private String paymentDate; // Date of payment

    @Column(name = "discount", nullable = false)
    private BigDecimal discount; // Discount applied on the total amount

    // GST Rate and Amounts (CGST, SGST, IGST)
    @Column(name = "gst_rate", nullable = false)
    private BigDecimal gstRate; // GST rate (e.g., 18% or 5%)

    @Column(name = "gst_amount", nullable = false)
    private BigDecimal gstAmount; // Total GST amount calculated (sum of CGST + SGST or IGST)

    // CGST (Intra-state transactions only)
    @Column(name = "cgst_amount", nullable = false)
    private BigDecimal cgstAmount; // CGST amount (if intra-state transaction)

    // SGST (Intra-state transactions only)
    @Column(name = "sgst_amount", nullable = false)
    private BigDecimal sgstAmount; // SGST amount (if intra-state transaction)

    // IGST (Inter-state transactions only)
    @Column(name = "igst_amount", nullable = false)
    private BigDecimal igstAmount; // IGST amount (if inter-state transaction)

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount; // Net amount after applying discount and GST


    @ManyToMany(fetch = FetchType.LAZY,cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "lab_billing",
            joinColumns = @JoinColumn(name = "billing_id"),
            inverseJoinColumns = @JoinColumn(name = "lab_id"))
    private Set<Lab> labs = new HashSet<>();


    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt; // Timestamp when the record is created

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
