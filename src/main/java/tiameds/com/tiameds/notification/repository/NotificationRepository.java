package tiameds.com.tiameds.notification.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tiameds.com.tiameds.notification.entity.Notification;
import tiameds.com.tiameds.notification.entity.NotificationStatus;
import tiameds.com.tiameds.notification.entity.NotificationType;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByLabIdOrderByCreatedAtDesc(Long labId);

    List<Notification> findByLabIdAndStatusOrderByCreatedAtDesc(Long labId, NotificationStatus status);

    List<Notification> findByLabIdAndRequestedByUserIdOrderByCreatedAtDesc(Long labId, Long requestedByUserId);

    long countByLabIdAndStatus(Long labId, NotificationStatus status);

    boolean existsByLabIdAndTypeAndEntityIdAndStatus(Long labId, NotificationType type, Long entityId,
                                                     NotificationStatus status);

    // Row lock so two admins cannot approve / reject the same request at the same time
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Notification n WHERE n.id = :id")
    Optional<Notification> findByIdForUpdate(@Param("id") Long id);
}
