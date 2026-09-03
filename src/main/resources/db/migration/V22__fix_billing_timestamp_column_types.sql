-- Same drift as V15 (lab_report/patient_visits created_at): billing.created_at
-- and billing.updated_at were never fixed to TIMESTAMP WITH TIME ZONE, so native
-- queries projecting them into an Instant-typed interface getter (e.g.
-- BillingRepository.GridReportRowProjection.getCreatedAt) return a raw Postgres
-- text value ("2026-08-24 10:35:52.701174") instead of a proper timestamptz,
-- which Jackson then fails to convert to java.time.Instant when serializing the
-- /lab-super-admin/stats/grid response. USING clause safely converts existing data.

ALTER TABLE billing
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE
        USING created_at::timestamptz;

ALTER TABLE billing
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE
        USING updated_at::timestamptz;
