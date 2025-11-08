package tiameds.com.tiameds.entity;

/**
 * Enum representing all entity types that support sequence generation.
 * Each entity type has a unique prefix used in generated codes.
 */
public enum EntityType {
    PATIENT("PAT"),
    VISIT("VIS"),
    BILLING("BIL"),
    TEST("TST"),
    HEALTH_PACKAGE("PKG"),
    TEST_REFERENCE("TREF"),
    SUPER_ADMIN_REFERENCE("SAREF"),
    REPORT("RPT"),
    TEST_DISCOUNT("TDISC"),
    TRANSACTION("TXN"),
    DOCTOR("DOC"),
    INSURANCE("INS"),
    SAMPLE("SAM"),
    SUPER_ADMIN_TEST("SAT"),
    VISIT_TEST_RESULT("VTR"),
    USER("USR");

    private final String prefix;

    EntityType(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns the prefix for this entity type.
     * @return the prefix string (e.g., "PAT", "VIS", "BIL")
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the entity name as a string (same as enum name).
     * @return the entity name
     */
    public String getEntityName() {
        return this.name();
    }
}

