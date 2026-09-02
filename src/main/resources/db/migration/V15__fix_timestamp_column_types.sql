-- Fix created_at columns on lab_report and patient_visits that were created as
-- character varying instead of TIMESTAMP WITH TIME ZONE (local dev DB schema drift).
-- Production already has correct types; the USING clause handles safe conversion.

ALTER TABLE lab_report
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE
        USING created_at::timestamptz;

ALTER TABLE patient_visits
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE
        USING created_at::timestamptz;
