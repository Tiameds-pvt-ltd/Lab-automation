package tiameds.com.tiameds.whatsapp.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import tiameds.com.tiameds.entity.VisitEntity;

import java.util.Optional;

/**
 * Read-only lookup used only by the WhatsApp module, so the existing VisitRepository is left untouched.
 * Returns the visit (with its patient fetched) only if the patient belongs to the given lab, which keeps
 * the lookup lab-isolated.
 */
public interface WhatsAppVisitLookupRepository extends Repository<VisitEntity, Long> {

    @Query("SELECT v FROM VisitEntity v JOIN FETCH v.patient p " +
            "WHERE v.visitId = :visitId " +
            "AND EXISTS (SELECT 1 FROM p.labs l WHERE l.id = :labId)")
    Optional<VisitEntity> findVisitInLab(@Param("visitId") Long visitId, @Param("labId") Long labId);
}
