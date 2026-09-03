-- ReportEntity maps reportJson/referenceRanges/testRows as jsonb
-- (@JdbcTypeCode(SqlTypes.JSON)), but lab_report has these as varchar,
-- causing Hibernate schema validation to fail on startup. All existing
-- non-null values are well-formed JSON, so a direct cast is safe.

ALTER TABLE lab_report
    ALTER COLUMN reference_ranges TYPE JSONB USING reference_ranges::jsonb;

ALTER TABLE lab_report
    ALTER COLUMN report_json TYPE JSONB USING report_json::jsonb;

ALTER TABLE lab_report
    ALTER COLUMN test_rows TYPE JSONB USING test_rows::jsonb;
