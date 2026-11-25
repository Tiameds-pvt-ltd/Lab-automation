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

    // Alerts meant only for super admins (cross-lab view).
    // A SUPERADMIN-targeted alert belongs to the super admin(s) who are members of the alert's lab; when the lab has
    // no super admin member at all, every super admin still sees it (so it can never be lost).
    String RESPECTIVE_SUPERADMIN =
            "n.targetRole = 'SUPERADMIN' AND ("
            + "EXISTS (SELECT 1 FROM Lab l JOIN l.members m WHERE l.id = n.labId AND m.id = :userId) "
            + "OR NOT EXISTS (SELECT 1 FROM Lab l2 JOIN l2.members m2 JOIN m2.roles r2 "
            + "WHERE l2.id = n.labId AND UPPER(r2.name) = 'SUPERADMIN'))";

    @Query("SELECT n FROM Notification n WHERE " + RESPECTIVE_SUPERADMIN + " ORDER BY n.createdAt DESC")
    List<Notification> findSuperAdminAlerts(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE " + RESPECTIVE_SUPERADMIN
            + " AND n.status = :status ORDER BY n.createdAt DESC")
    List<Notification> findSuperAdminAlertsByStatus(@Param("userId") Long userId,
                                                    @Param("status") NotificationStatus status);

    @Query("SELECT COUNT(n) FROM Notification n WHERE " + RESPECTIVE_SUPERADMIN + " AND n.status = :status")
    long countSuperAdminAlertsByStatus(@Param("userId") Long userId, @Param("status") NotificationStatus status);

    // Is this super admin a member of the lab (used to check a single alert)
    @Query("SELECT COUNT(m) > 0 FROM Lab l JOIN l.members m WHERE l.id = :labId AND m.id = :userId")
    boolean isMemberOfLab(@Param("labId") Long labId, @Param("userId") Long userId);

    @Query("SELECT COUNT(m) > 0 FROM Lab l JOIN l.members m JOIN m.roles r "
            + "WHERE l.id = :labId AND UPPER(r.name) = 'SUPERADMIN'")
    boolean labHasSuperAdminMember(@Param("labId") Long labId);

    // Alerts meant for the lab's desk users (e.g. "report completed, please print")
    List<Notification> findByLabIdAndTargetRoleOrderByCreatedAtDesc(Long labId, String targetRole);

    List<Notification> findByLabIdAndTargetRoleAndStatusOrderByCreatedAtDesc(Long labId, String targetRole,
                                                                             NotificationStatus status);

    long countByLabIdAndTargetRoleAndStatus(Long labId, String targetRole, NotificationStatus status);

    // What a plain ADMIN may see: everything except the alerts reserved for super admins and for desk users
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.labId = :labId AND n.status = :status "
            + "AND (n.targetRole IS NULL OR n.targetRole NOT IN ('SUPERADMIN', 'DESKROLE'))")
    long countVisibleToAdmins(@Param("labId") Long labId, @Param("status") NotificationStatus status);

    boolean existsByLabIdAndTypeAndEntityIdAndStatus(Long labId, NotificationType type, Long entityId,
                                                     NotificationStatus status);

    // Row lock so two admins cannot approve / reject the same request at the same time
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Notification n WHERE n.id = :id")
    Optional<Notification> findByIdForUpdate(@Param("id") Long id);
}
