package tiameds.com.tiameds.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import tiameds.com.tiameds.repository.GenderConverter;

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

    @Column(nullable = true)
    private String testDescription;

    @Column(nullable = true)
    private String units;

    @Convert(converter = GenderConverter.class)
    @Column(name = "gender")
    private Gender gender;

    @Column(nullable = true)
    private Double minReferenceRange;

    @Column(nullable = true)
    private Double maxReferenceRange;

    @Column(nullable = true)
    private Integer ageMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private AgeUnit minAgeUnit;

    @Column(nullable = true)
    private Integer ageMax;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private AgeUnit maxAgeUnit;

    @Column(nullable = true)
    private String remarks;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String reportJson;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String referenceRanges;

    @Column(nullable = true)
    private String createdBy;

    @Column(nullable = true)
    private String updatedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "super_admin_reference_code", unique = true)
    private String superAdminReferenceCode;
}

