package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tiameds.com.tiameds.entity.TestReferenceEntity;

@Repository
public interface LabTestReferenceLinkRepository extends org.springframework.data.repository.Repository<TestReferenceEntity, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO lab_test_references (lab_id, test_reference_id)
            VALUES (:labId, :referenceId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void linkLabToReference(@Param("labId") Long labId, @Param("referenceId") Long referenceId);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM lab_test_references
            WHERE lab_id = :labId AND test_reference_id = :referenceId
            """, nativeQuery = true)
    int unlinkLabFromReference(@Param("labId") Long labId, @Param("referenceId") Long referenceId);
}

