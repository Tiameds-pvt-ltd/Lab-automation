package tiameds.com.tiameds.notification.entity;

public enum NotificationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    /** Used by information-only alerts (e.g. VISIT_CANCELLED) once an admin has seen them. */
    ACKNOWLEDGED
}
