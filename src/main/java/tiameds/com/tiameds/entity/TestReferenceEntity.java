package tiameds.com.tiameds.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tiameds.com.tiameds.repository.GenderConverter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "test_reference")
@Data
public class TestReferenceEntity {

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

    @Convert(converter = GenderConverter.class)
    @Column(name = "gender")
    private Gender gender;


    @Column(nullable = true)
    private Double minReferenceRange;

    @Column(nullable = true)
    private Double maxReferenceRange;

    @Column(nullable = false)
    private Integer ageMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private AgeUnit minAgeUnit;

    @Column(nullable = false)
    private Integer ageMax;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private AgeUnit maxAgeUnit;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToMany(mappedBy = "testReferences", fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonBackReference
    private Set<Lab> labs = new HashSet<>();

}
