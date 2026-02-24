package tiameds.com.tiameds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "report_settings", uniqueConstraints = @UniqueConstraint(columnNames = "lab_id"))
public class ReportSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_id", nullable = false, unique = true)
    private Lab lab;

    @Column(name = "template_id", nullable = false, length = 30)
    private String templateId = "templateA";

    @Column(name = "header_enabled", nullable = false)
    private Boolean headerEnabled = true;

    @Column(name = "header_required", nullable = false)
    private Boolean headerRequired = false;

    @Column(name = "font_size", nullable = false)
    private Integer fontSize = 12;

    @Column(name = "text_size", nullable = false, length = 10)
    private String textSize = "Medium";

    @Column(name = "text_color", nullable = false, length = 10)
    private String textColor = "#111827";

    @Column(name = "signature_placement", nullable = false, length = 20)
    private String signaturePlacement = "bottom-right";

    @Column(name = "signature_columns", nullable = false)
    private Integer signatureColumns = 2;

    @Column(name = "disclaimer_enabled", nullable = false)
    private Boolean disclaimerEnabled = true;

    @Column(name = "disclaimer_text", nullable = false, columnDefinition = "TEXT")
    private String disclaimerText = "This laboratory report is intended for clinical correlation only. Results should be interpreted by a qualified medical professional.";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "reportSettings", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ReportRoleSetting> roles = new ArrayList<>();

    public Long getLabId() {
        return lab != null ? lab.getId() : null;
    }
}
