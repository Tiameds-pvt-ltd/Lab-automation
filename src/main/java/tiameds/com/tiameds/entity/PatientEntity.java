package tiameds.com.tiameds.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "patients")
public class PatientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "zip")
    private String zip;

    @Column(name = "Blood_Group")
    private String bloodGroup;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "age")
    private String age;

    @Column(name="gender")
    private String gender;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    // One patient can have multiple visits

    @OneToMany(mappedBy = "patient", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<VisitEntity> visits = new HashSet<>();

    // Guardian relationship - stores the ID in guardian_id column
    @Column(name = "guardian_id")
    private Long guardianId;

    // Guardian relationship - object reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", referencedColumnName = "patient_id", insertable = false, updatable = false)
    private PatientEntity guardian;


    //patient code for self reference
    @Column(name = "patient_code", unique = true)
    private String patientCode;

    // One patient can have multiple labs and one lab can have multiple patients.
    // No cascade: the Lab side is always an already-persisted entity fetched by id before
    // this association is ever touched, so cascading PERSIST/MERGE to it is never useful -
    // and combined with the mirrored cascade that used to be on Lab.patients (removed), it
    // put the new, still-transient Patient into a cascade cycle (Patient -> labs -> Lab ->
    // patients -> back to the same transient Patient) that corrupted the flush before the
    // IDENTITY-generated id was set, causing "AssertionFailure: null identifier (PatientEntity)"
    // on every new-patient save. Writing the lab_patients join-table row for this collection
    // does not require cascade - that happens automatically for the owning side regardless.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "lab_patients",
            joinColumns = @JoinColumn(name = "patient_id"),
            inverseJoinColumns = @JoinColumn(name = "lab_id")
    )
    @JsonManagedReference
    private Set<Lab> labs = new HashSet<>();

    public long getId() {
        return patientId;
    }

    // Custom method to handle both guardian fields
    public void setGuardian(PatientEntity guardian) {
        this.guardian = guardian;
        this.guardianId = (guardian != null) ? guardian.getPatientId() : null;
    }
}


