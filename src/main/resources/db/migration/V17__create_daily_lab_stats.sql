-- Rollup table for the super-admin dashboard (/lab-super-admin/stats/all).
-- Pre-aggregates per (lab_id, stat_date) so the dashboard reads small pre-summed
-- rows instead of scanning billing/patient_visits/visit_test_result live on every request.
-- Purely additive: does not alter any existing table.
--
-- No FK to labs(lab_id): some environments' labs table (built up via ddl-auto=update
-- over time, same drift as V12-V16) has no unique constraint on lab_id, which a
-- REFERENCES clause requires. Skipping the FK avoids depending on that and keeps this
-- migration additive-only — it never touches the existing labs table.

CREATE TABLE IF NOT EXISTS daily_lab_stats (
    lab_id              BIGINT NOT NULL,
    stat_date           DATE NOT NULL,
    test_count          BIGINT NOT NULL DEFAULT 0,
    reports_generated   BIGINT NOT NULL DEFAULT 0,
    pending_samples     BIGINT NOT NULL DEFAULT 0,
    patient_count       BIGINT NOT NULL DEFAULT 0,
    paid_revenue        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    due_revenue         NUMERIC(14, 2) NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (lab_id, stat_date)
);

CREATE INDEX IF NOT EXISTS idx_daily_lab_stats_date ON daily_lab_stats (stat_date);
