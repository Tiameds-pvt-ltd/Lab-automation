-- Same drift pattern as prior migrations: these entities map the column as
-- java.time.LocalDate, but the columns were created as varchar, causing
-- Hibernate schema validation to fail on startup one table at a time.

ALTER TABLE patients
    ALTER COLUMN date_of_birth TYPE DATE USING date_of_birth::date;

ALTER TABLE patient_visits
    ALTER COLUMN visit_date TYPE DATE USING visit_date::date;

ALTER TABLE patient_visits
    ALTER COLUMN visit_cancellation_date TYPE DATE USING visit_cancellation_date::date;
