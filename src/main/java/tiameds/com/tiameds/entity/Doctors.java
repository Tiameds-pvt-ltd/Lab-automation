package tiameds.com.tiameds.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Table(name = "doctors")
public class Doctors {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doctor_id")
    private long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    @Column(nullable = false)
    private String name;

    @Email(message = "Invalid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Speciality is required")
    @Size(max = 50, message = "Speciality cannot exceed 50 characters")
    @Column(nullable = false)
    private String speciality;

    @NotBlank(message = "Qualification is required")
    @Size(max = 100, message = "Qualification cannot exceed 100 characters")
    @Column(nullable = false)
    private String qualification;

    @NotBlank(message = "Hospital affiliation is required")
    @Size(max = 100, message = "Hospital affiliation cannot exceed 100 characters")
    @Column(nullable = false)
    private String hospitalAffiliation;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotBlank(message = "Phone is required")
    @Size(max = 10, message = "Phone number cannot exceed 15 characters")
    @Column(nullable = false)
    private String phone;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    @Column(nullable = false)
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City cannot exceed 50 characters")
    @Column(nullable = false)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 50, message = "State cannot exceed 50 characters")
    @Column(nullable = false)
    private String state;

    @NotBlank(message = "Country is required")
    @Size(max = 50, message = "Country cannot exceed 50 characters")
    @Column(nullable = false)
    private String country;

    @ManyToMany(mappedBy = "doctors", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonBackReference
    private Set<Lab> labs = new HashSet<>();

    @OneToMany(mappedBy = "doctor", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonBackReference
    private Set<VisitEntity> visits = new HashSet<>();


    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

   // createdby and updateby
    @Column(name = "created_by")
    private String CreatedBy;

    @Column(name = "updated_by")
    private String UpdatedBy;

    @Column(name = "doctor_code", unique = true)
    private String doctorCode;

}
