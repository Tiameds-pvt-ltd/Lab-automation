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
@Table(name = "parameter_master")
public class ParameterMasterEntity {

    @Id
    @Column(name = "parameter_id")
    private Integer parameterId;

    @Column(name = "parameter_name", nullable = false, length = 150)
    private String parameterName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
