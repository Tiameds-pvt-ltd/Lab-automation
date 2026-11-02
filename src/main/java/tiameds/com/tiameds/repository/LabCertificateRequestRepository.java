package tiameds.com.tiameds.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.LabApprovalStatus;
import tiameds.com.tiameds.entity.LabCertificateRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabCertificateRequestRepository extends JpaRepository<LabCertificateRequest, Long> {

    List<LabCertificateRequest> findByLabIdOrderByRequestedAtDesc(Long labId);

    List<LabCertificateRequest> findByStatusOrderByRequestedAtAsc(LabApprovalStatus status);

    boolean existsByLabIdAndDocumentTypeAndStatus(Long labId, String documentType, LabApprovalStatus status);

    // Row lock so two reviewers cannot approve / reject the same request at the same time
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM LabCertificateRequest r WHERE r.id = :id")
    Optional<LabCertificateRequest> findByIdForUpdate(@Param("id") Long id);
}
