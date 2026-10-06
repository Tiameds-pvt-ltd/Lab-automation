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
@Table(name = "tbl_parameter_master")
public class ParameterMaster {

    @Id
    @Column(name = "parameter_id")
    private Integer parameterId;

    @Column(name = "parameter_name", nullable = false, length = 150)
    private String parameterName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private TestsMaster test;
}
