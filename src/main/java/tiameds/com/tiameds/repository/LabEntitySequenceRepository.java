package tiameds.com.tiameds.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.LabEntitySequence;
import tiameds.com.tiameds.entity.LabEntitySequenceId;

import java.util.Optional;

/**
 * Repository for LabEntitySequence with pessimistic locking support.
 * Uses PESSIMISTIC_WRITE lock to ensure transaction-safe sequence generation.
 */
@Repository
public interface LabEntitySequenceRepository extends JpaRepository<LabEntitySequence, LabEntitySequenceId> {

    /**
     * Finds a sequence record by lab ID and entity name with pessimistic write lock.
     * This ensures that concurrent transactions will wait for the lock to be released,
     * preventing race conditions in sequence generation.
     *
     * @param labId the lab ID
     * @param entityName the entity name
     * @return Optional containing the sequence record if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM LabEntitySequence s WHERE s.id.labId = :labId AND s.id.entityName = :entityName")
    Optional<LabEntitySequence> findByLabIdAndEntityNameWithLock(
            @Param("labId") Long labId,
            @Param("entityName") String entityName
    );

    /**
     * Finds a sequence record by lab ID and entity name without locking.
     * Use this for read-only operations.
     *
     * @param labId the lab ID
     * @param entityName the entity name
     * @return Optional containing the sequence record if found
     */
    @Query("SELECT s FROM LabEntitySequence s WHERE s.id.labId = :labId AND s.id.entityName = :entityName")
    Optional<LabEntitySequence> findByLabIdAndEntityName(
            @Param("labId") Long labId,
            @Param("entityName") String entityName
    );
}

