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

    //    ======================new feilds=========================

    @Column(name = "lab_logo",nullable = true) // Nullable if logo upload is optional
    private String labLogo;

    @Column(name = "license_number",nullable = false)
    private String licenseNumber;

    @Column(name = "lab_type",nullable = false)
    private String labType;

    @Column(name = "lab_zip",nullable = false)
    private String labZip;

    @Column(name = "lab_country",nullable = false)
    private String labCountry;

    @Column(name = "lab_phone",nullable = false)
    private String labPhone;

    @Column(name = "lab_email",nullable = false)
    private String labEmail;

    @Column(name = "director_name",nullable = false)
    private String directorName;

    @Column(name = "director_email",nullable = false)
    private String directorEmail;

    @Column(name = "director_phone",nullable = false)
    private String directorPhone;

    @Column(name = "certification_body",nullable = false)
    private String certificationBody;

    @Column(name = "lab_certificate",nullable = false)
    private String labCertificate;

    @Column(name = "director_govt_id",nullable = false)
    private String directorGovtId;

    @Column(name = "lab_business_registration",nullable = false)
    private String labBusinessRegistration;

    @Column(name = "lab_license",nullable = false)
    private String labLicense;

    @Column(name = "tax_id",nullable = false)
    private String taxId;

    @Column(name = "lab_accreditation",nullable = false)
    private String labAccreditation;

    @Column(name = "data_privacy_agreement",nullable = false)
    private Boolean dataPrivacyAgreement;

    //    ======================new feilds=========================

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


    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "lab_test_references",
            joinColumns = @JoinColumn(name = "lab_id"),
            inverseJoinColumns = @JoinColumn(name = "test_reference_id")
    )
    @JsonManagedReference
    private Set<TestReferenceEntity> testReferences = new HashSet<>();


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

    public void addTestReference(TestReferenceEntity entity) {
        this.testReferences.add(entity);
        entity.getLabs().add(this);
    }

    public void removeTestReference(TestReferenceEntity testReferenceEntity) {
        this.testReferences.remove(testReferenceEntity);
        testReferenceEntity.getLabs().remove(this);
    }

}

