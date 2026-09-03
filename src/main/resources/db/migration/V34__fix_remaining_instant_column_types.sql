-- Same drift pattern as V15/V22/V28/V29/V33: these entities map the column
-- as java.time.Instant, but the columns were created as varchar, causing
-- Hibernate schema validation to fail on startup one table at a time.

ALTER TABLE password_reset_rate_limits
    ALTER COLUMN window_start TYPE TIMESTAMP WITH TIME ZONE USING window_start::timestamptz;

ALTER TABLE verification_tokens
    ALTER COLUMN expiry_time TYPE TIMESTAMP WITH TIME ZONE USING expiry_time::timestamptz;

ALTER TABLE patient_visits
    ALTER COLUMN visit_cancellation_time TYPE TIMESTAMP WITH TIME ZONE USING visit_cancellation_time::timestamptz;

ALTER TABLE patient_visits
    ALTER COLUMN visit_time TYPE TIMESTAMP WITH TIME ZONE USING visit_time::timestamptz;
