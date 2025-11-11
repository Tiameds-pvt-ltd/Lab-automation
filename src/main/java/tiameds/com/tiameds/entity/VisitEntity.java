package tiameds.com.tiameds.entity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "patient_visits")
public class VisitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visit_id")
    private Long visitId;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "visit_type", nullable = false)
    private String visitType; // IN-PATIENT, OUT-PATIENT

    @Column(name = "visit_status", nullable = false)
    private String visitStatus;

    @Column(name = "visit_description")
    private String visitDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "patient_visit_tests",
            joinColumns = @JoinColumn(name = "visit_id"),
            inverseJoinColumns = @JoinColumn(name = "test_id")
    )
    @JsonBackReference
    private Set<Test> tests = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "billing_id", referencedColumnName = "billing_id")
    private BillingEntity billing;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "patient_visit_packages",
            joinColumns = @JoinColumn(name = "visit_id"),
            inverseJoinColumns = @JoinColumn(name = "package_id")
    )
    @JsonBackReference
    private Set<HealthPackage> packages = new HashSet<>();


    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "visit_insurance", // Define the join table name
            joinColumns = @JoinColumn(name = "visit_id"),
            inverseJoinColumns = @JoinColumn(name = "insurance_id")
    )
    @JsonManagedReference
    private Set<InsuranceEntity> insurance = new HashSet<>();


    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<VisitSample> visitSamples = new HashSet<>();

    // Convenience method to get samples
    public Set<SampleEntity> getSamples() {
        return visitSamples.stream()
                .map(VisitSample::getSample)
                .collect(Collectors.toSet());
    }

    // Convenience method to add sample
    public void addSample(SampleEntity sample) {
        VisitSample visitSample = new VisitSample();
        visitSample.setVisit(this);
        visitSample.setSample(sample);
        this.visitSamples.add(visitSample);
    }

    // Convenience method to remove sample
    public void removeSample(SampleEntity sample) {
        visitSamples.removeIf(vs -> vs.getSample().equals(sample));
    }


    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "lab_visit",
            joinColumns = @JoinColumn(name = "visit_id"),
            inverseJoinColumns = @JoinColumn(name = "lab_id"))
    private Set<Lab> labs = new HashSet<>();


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = true)
    private Doctors doctor;

    // new data for visit table------------------------

    @Column(name = "visit_cancellation_reason")
    private String visitCancellationReason;

    @Column(name = "visit_cancellation_date")
    private LocalDate visitCancellationDate;

    @Column(name = "visit_cancellation_by")
    private String visitCancellationBy;

    @Column(name = "visit_cancellation_time")
    private LocalDateTime visitCancellationTime;

//    visitTime
    @Column(name = "visit_time")
    private LocalDateTime visitTime;

    @Column(name ="created_by")
    private String createdBy;

    @Column(name ="updated_by")
    private String updatedBy;
//
//    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true)
//    private Set<VisitTestResult> testResults = new HashSet<>();

    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VisitTestResult> testResults = new HashSet<>();


    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "visit_code", unique = true)
    private String visitCode;

}
