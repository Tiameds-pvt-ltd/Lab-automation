-- Dashboard Performance Indexes
-- Flyway runs this automatically on next app startup (once only).
-- Note: CONCURRENTLY is omitted here because Flyway wraps migrations in a
-- transaction and PostgreSQL disallows CONCURRENTLY inside one.
-- IF NOT EXISTS makes every statement safe to re-run manually if needed.

-- Functional indexes for LOWER(visit_status) / LOWER(test_status)
-- Every dashboard query filters: WHERE LOWER(pv.visit_status) != 'cancelled'
-- Without these, PostgreSQL cannot use a btree index and does a full table scan.
CREATE INDEX IF NOT EXISTS idx_patient_visits_status_lower
    ON patient_visits (LOWER(visit_status));

CREATE INDEX IF NOT EXISTS idx_visit_test_result_status_lower
    ON visit_test_result (LOWER(test_status));

-- billing.created_at — every date-range filter on billing
CREATE INDEX IF NOT EXISTS idx_billing_created_at
    ON billing (created_at);

-- lab_billing(billing_id) — JOIN hot path from billing to lab
CREATE INDEX IF NOT EXISTS idx_lab_billing_billing_id
    ON lab_billing (billing_id);

-- labs.created_by — tenant-isolation filter on every super-admin query
CREATE INDEX IF NOT EXISTS idx_labs_created_by
    ON labs (created_by);

-- lab_visit(visit_id) — JOIN path for test/visit aggregations
CREATE INDEX IF NOT EXISTS idx_lab_visit_visit_id
    ON lab_visit (visit_id);

-- Compound index: visit_test_result(visit_id, LOWER(test_status))
-- Covers: WHERE vtr.visit_id = ? AND LOWER(vtr.test_status) = 'active'
CREATE INDEX IF NOT EXISTS idx_vtr_visit_id_status
    ON visit_test_result (visit_id, LOWER(test_status));

-- visit_test_result.created_at — date-range queries on tests
CREATE INDEX IF NOT EXISTS idx_vtr_created_at
    ON visit_test_result (created_at);

-- patient_visits.created_at — date-range filter hot path
CREATE INDEX IF NOT EXISTS idx_patient_visits_created_at
    ON patient_visits (created_at);

-- patient_visits.billing_id — JOIN from billing to visit
CREATE INDEX IF NOT EXISTS idx_patient_visits_billing_id
    ON patient_visits (billing_id);

-- billing_transaction.billing_id — aggregation subquery grouping
CREATE INDEX IF NOT EXISTS idx_billing_transaction_billing_id
    ON billing_transaction (billing_id);
