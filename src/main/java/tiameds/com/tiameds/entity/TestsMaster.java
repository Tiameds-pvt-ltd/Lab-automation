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

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tbl_tests_master")
public class TestsMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tests_master_seq")
    @SequenceGenerator(name = "tests_master_seq", sequenceName = "tbl_tests_master_test_id_seq", allocationSize = 1)
    @Column(name = "test_id")
    private Integer testId;

    @Column(name = "test_code", nullable = false, unique = true, length = 20)
    private String testCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 1 = active, 0 = inactive (soft delete)
    @Column(name = "is_active", nullable = false)
    private int isActive = 1;
}
