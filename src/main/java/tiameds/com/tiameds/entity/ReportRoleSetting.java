package tiameds.com.tiameds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "report_role_settings")
public class ReportRoleSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_setting_id", nullable = false)
    private ReportSettings reportSettings;

    @Column(nullable = false, length = 100)
    private String role;

    @Column(name = "display_name", length = 200)
    private String displayName = "";

    @Column(length = 200)
    private String designation = "";

    @Column(name = "signature_url", columnDefinition = "TEXT")
    private String signatureUrl = "";

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
