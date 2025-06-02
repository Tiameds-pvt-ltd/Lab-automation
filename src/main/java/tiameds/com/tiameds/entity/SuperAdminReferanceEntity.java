package tiameds.com.tiameds.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "super_admin_test_referance")
public class SuperAdminReferanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_reference_id")
    private Long id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String testName;

    @Column(nullable = false)
    private String testDescription;

    @Column
    private String units;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private Double minReferenceRange;

    @Column(nullable = false)
    private Double maxReferenceRange;

    @Column(nullable = false)
    private Integer ageMin;

    @Column(nullable = false)
    private Integer ageMax;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
