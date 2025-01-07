package tiameds.com.tiameds.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "labs")
public class Lab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lab_id")
    private long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY) // Good practice to use lazy fetching unless absolutely needed
    @JoinColumn(name = "created_by", referencedColumnName = "user_id")
    @JsonBackReference
    private User createdBy;


    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "lab_members",
            joinColumns = @JoinColumn(name = "lab_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonManagedReference
    private Set<User> members = new HashSet<>();

    //test
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "lab_tests",
            joinColumns = @JoinColumn(name = "lab_id"),
            inverseJoinColumns = @JoinColumn(name = "test_id")
    )
    @JsonManagedReference
    private Set<Test> tests = new HashSet<>();


    //package
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "lab_packages",
            joinColumns = @JoinColumn(name = "lab_id"),
            inverseJoinColumns = @JoinColumn(name = "package_id")
    )
    @JsonManagedReference(value = "package-labs")
    private Set<HealthPackage> healthPackages = new HashSet<>();


    public void addTest(Test test) {
        this.tests.add(test);
        test.getLabs().add(this);
    }

    public void removeTest(Test test) {
        this.tests.remove(test);
        test.getLabs().remove(this);
    }


    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "lab_doctors",
            joinColumns = @JoinColumn(name = "lab_id"),
            inverseJoinColumns = @JoinColumn(name = "doctor_id")
    )
    @JsonManagedReference
    private Set<Doctors> doctors = new HashSet<>();


    //insurance
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "lab_insurance",
            joinColumns = @JoinColumn(name = "lab_id"),
            inverseJoinColumns = @JoinColumn(name = "insurance_id")
    )
    @JsonManagedReference
    private Set<InsuranceEntity> insurance = new HashSet<>();


    public String getCreatedByName() {
        return this.createdBy.getFullName();
    }


    //patient -> in one lab multiple patients can be registered
    @ManyToMany(mappedBy = "labs", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonBackReference
    private Set<PatientEntity> patients = new HashSet<>();

    public String getInsurances() {
        return this.insurance.stream().map(InsuranceEntity::getName).reduce((a, b) -> a + ", " + b).orElse("");
    }

}

