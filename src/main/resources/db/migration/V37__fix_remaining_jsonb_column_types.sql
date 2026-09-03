-- Same drift as V32: SuperAdminReferanceEntity and TestReferenceEntity map
-- these fields as jsonb (@JdbcTypeCode(SqlTypes.JSON)), but the columns were
-- created as varchar, causing Hibernate schema validation to fail on
-- startup. All existing non-null values are well-formed JSON.

ALTER TABLE super_admin_test_referance
    ALTER COLUMN report_json TYPE JSONB USING report_json::jsonb;

ALTER TABLE super_admin_test_referance
    ALTER COLUMN reference_ranges TYPE JSONB USING reference_ranges::jsonb;

ALTER TABLE test_reference
    ALTER COLUMN report_json TYPE JSONB USING report_json::jsonb;

ALTER TABLE test_reference
    ALTER COLUMN reference_ranges TYPE JSONB USING reference_ranges::jsonb;

ALTER TABLE test_reference
    ALTER COLUMN dropdown TYPE JSONB USING dropdown::jsonb;

ALTER TABLE test_reference
    ALTER COLUMN impression TYPE JSONB USING impression::jsonb;
