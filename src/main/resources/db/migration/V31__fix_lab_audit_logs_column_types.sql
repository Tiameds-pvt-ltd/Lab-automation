-- LabAuditLogs entity maps id -> UUID, timestamp -> LocalDateTime, and
-- fieldChanged/oldValue/newValue -> jsonb, but this table was created with
-- varchar columns across the board, causing Hibernate schema validation to
-- fail on startup. Existing data is well-formed (valid UUID strings, valid
-- JSON), so a direct cast is safe.

ALTER TABLE lab_audit_logs
    ALTER COLUMN id TYPE UUID USING id::uuid;

ALTER TABLE lab_audit_logs
    ALTER COLUMN timestamp TYPE TIMESTAMP USING timestamp::timestamp;

ALTER TABLE lab_audit_logs
    ALTER COLUMN field_changed TYPE JSONB USING field_changed::jsonb;

ALTER TABLE lab_audit_logs
    ALTER COLUMN old_value TYPE JSONB USING old_value::jsonb;

ALTER TABLE lab_audit_logs
    ALTER COLUMN new_value TYPE JSONB USING new_value::jsonb;
