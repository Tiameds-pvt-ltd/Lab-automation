package tiameds.com.tiameds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tbl_description_master")
public class DescriptionMaster {

    @Id
    @Column(name = "description_id")
    private Integer descriptionId;

    @Column(name = "description_name", nullable = false, length = 100)
    private String descriptionName;

    @Column(name = "result_type", nullable = false, length = 20)
    private String resultType;

    @Column(name = "options", columnDefinition = "TEXT")
    private String options;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parameter_id", nullable = false)
    private ParameterMaster parameter;
}
