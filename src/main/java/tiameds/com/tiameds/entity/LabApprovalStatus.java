package tiameds.com.tiameds.entity;

/**
 * Review state of a lab that was registered through onboarding.
 * Labs created any other way are APPROVED from the start.
 */
public enum LabApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
