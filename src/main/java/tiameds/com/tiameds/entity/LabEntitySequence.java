package tiameds.com.tiameds.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing the sequence counter for each entity type per lab.
 * Used to generate unique, sequential codes like PAT-00001, VIS-00001, etc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lab_entity_sequence")
public class LabEntitySequence {

    @EmbeddedId
    private LabEntitySequenceId id;

    @Column(name = "last_number", nullable = false)
    private Long lastNumber = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Convenience method to get lab ID from embedded ID.
     */
    public Long getLabId() {
        return id != null ? id.getLabId() : null;
    }

    /**
     * Convenience method to get entity name from embedded ID.
     */
    public String getEntityName() {
        return id != null ? id.getEntityName() : null;
    }

    /**
     * Convenience method to set lab ID in embedded ID.
     */
    public void setLabId(Long labId) {
        if (id == null) {
            id = new LabEntitySequenceId();
        }
        id.setLabId(labId);
    }

    /**
     * Convenience method to set entity name in embedded ID.
     */
    public void setEntityName(String entityName) {
        if (id == null) {
            id = new LabEntitySequenceId();
        }
        id.setEntityName(entityName);
    }
}

