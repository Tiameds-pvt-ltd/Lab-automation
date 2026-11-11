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
    /** Information only: an ADMIN created a user in a lab or added an existing user to it; super admins are told. */
    USER_ADDED_TO_LAB,
    /** An ADMIN's request to create a user in a lab / add an existing user to it, held until a SUPERADMIN approves. */
    USER_ADD_REQUEST,
    /** An ADMIN's request to update a lab user's details / roles, held until a SUPERADMIN approves. */
    USER_UPDATE_REQUEST,
    /** A test add / update / delete held for approval (ADMIN proposes, a SUPERADMIN approves or rejects). */
    TEST_CHANGE_REQUEST,
    /** A test reference add / update / delete / delete-all held for approval (technician or desk -> admin, admin -> super admin). */
    TEST_REFERENCE_CHANGE_REQUEST
}
