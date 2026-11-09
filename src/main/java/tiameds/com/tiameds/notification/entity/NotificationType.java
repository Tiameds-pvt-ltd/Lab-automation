package tiameds.com.tiameds.notification.entity;

/** What a notification is about. New alert kinds are added here. */
public enum NotificationType {
    /** A lab user asked to change a patient's details; an ADMIN of the lab approves or rejects. */
    PATIENT_EDIT_REQUEST,
    /** Information only: a non-admin cancelled a visit. Admins just acknowledge it, there is nothing to approve. */
    VISIT_CANCELLED,
    /** Information only: a non-admin added / updated / deleted / bulk-changed test reference data. */
    TEST_REFERENCE_CHANGED,
    /** Information only: a user added / updated / deleted a lab test (price list). */
    TEST_CHANGED,
    /** A test add / update / delete held for approval (ADMIN proposes, a SUPERADMIN approves or rejects). */
    TEST_CHANGE_REQUEST,
    /** A test reference add / update / delete / delete-all held for approval (technician or desk -> admin, admin -> super admin). */
    TEST_REFERENCE_CHANGE_REQUEST
}
