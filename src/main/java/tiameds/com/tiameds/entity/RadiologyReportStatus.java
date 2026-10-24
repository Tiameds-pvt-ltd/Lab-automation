package tiameds.com.tiameds.entity;

/**
 * Life cycle of a radiology report.
 * DRAFT   - being written, freely editable.
 * FINAL   - signed off; changes only through an amendment (reason + history row).
 * AMENDED - changed after it was finalised; same rules as FINAL.
 */
public enum RadiologyReportStatus {
    DRAFT,
    FINAL,
    AMENDED;

    public boolean isLocked() {
        return this != DRAFT;
    }
}
