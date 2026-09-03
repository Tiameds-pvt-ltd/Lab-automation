-- VisitTestResult entity maps created_at/updated_at as LocalDateTime, but
-- the columns were created as varchar (missed in the V29 batch fix),
-- causing Hibernate schema validation to fail on startup.

ALTER TABLE visit_test_result
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;
