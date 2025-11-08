package tiameds.com.tiameds.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Embedded ID class for LabEntitySequence.
 * Composite key consisting of labId and entityName.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class LabEntitySequenceId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "lab_id", nullable = false)
    private Long labId;

    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;
}

