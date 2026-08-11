package tiameds.com.tiameds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ai_clinical_observations")
public class AiClinicalObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id", nullable = false)
    private VisitEntity visit;

    @Column(name = "provisional_diagnosis", columnDefinition = "TEXT")
    private String provisionalDiagnosis;

    @Column(name = "clinical_interpretation", columnDefinition = "TEXT")
    private String clinicalInterpretation;

    @Column(name = "doctor_to_visit")
    private String doctorToVisit;

    @Column(name = "patient_interpretation", columnDefinition = "TEXT")
    private String patientInterpretation;

    @Column(name = "tips", columnDefinition = "TEXT")
    private String tips;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
