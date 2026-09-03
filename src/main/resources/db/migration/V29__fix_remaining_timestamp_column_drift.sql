-- Same drift as V15/V22/V28: many created_at/updated_at columns on this local
-- dev DB were created as character varying instead of a proper timestamp type,
-- causing Hibernate schema validation to fail on startup one table at a time.
-- This migration fixes every remaining table in one pass, matching each
-- column's actual JPA field type: LocalDateTime -> timestamp (no tz),
-- Instant -> timestamp with time zone. USING clause safely converts existing
-- data.

-- LocalDateTime-mapped columns -> TIMESTAMP (no time zone)
ALTER TABLE doctors
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE health_packages
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE lab_entity_sequence
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE labs
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE modules
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE patient_visit_sample
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE report_role_settings
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE report_settings
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE roles
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE sample_entity
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE super_admin_test_pricelistentity
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE super_admin_test_referance
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE test_discount
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE test_reference
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE tests
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE users
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

-- Instant-mapped columns -> TIMESTAMP WITH TIME ZONE
ALTER TABLE lab_report
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE USING updated_at::timestamptz;

ALTER TABLE otps
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at::timestamptz;

ALTER TABLE password_reset_rate_limits
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at::timestamptz;

ALTER TABLE password_reset_tokens
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at::timestamptz;

ALTER TABLE patient_visits
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE USING updated_at::timestamptz;

ALTER TABLE patients
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at::timestamptz,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE USING updated_at::timestamptz;

ALTER TABLE refresh_tokens
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at::timestamptz;

ALTER TABLE verification_tokens
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at::timestamptz;
