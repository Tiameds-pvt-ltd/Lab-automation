package tiameds.com.tiameds.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.entity.LabApprovalStatus;
import tiameds.com.tiameds.entity.TempLab;

import java.util.List;
import java.util.Optional;

@Repository
public interface TempLabRepository extends JpaRepository<TempLab, Long> {

    List<TempLab> findByApprovalStatusOrderByApprovalRequestedAtAsc(LabApprovalStatus approvalStatus);

    boolean existsByEmailIgnoreCaseAndApprovalStatus(String email, LabApprovalStatus approvalStatus);

    boolean existsByUsernameAndApprovalStatus(String username, LabApprovalStatus approvalStatus);

    boolean existsByLabNameIgnoreCaseAndApprovalStatus(String labName, LabApprovalStatus approvalStatus);

    // add-lab requests of one user, newest first
    List<TempLab> findByExistingUserIdOrderByApprovalRequestedAtDesc(Long existingUserId);

    // Latest submission for a login identifier (username, or email ignoring case)
    Optional<TempLab> findFirstByUsernameOrEmailIgnoreCaseOrderByApprovalRequestedAtDesc(String username, String email);
}
