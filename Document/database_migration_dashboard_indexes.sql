-- ============================================================
-- Dashboard Performance Indexes
-- Run once on production (and local dev) to eliminate full-table
-- sequential scans caused by LOWER() on status columns and by
-- missing compound indexes on the hot join paths.
-- ============================================================

-- 1. Functional indexes for LOWER(visit_status) and LOWER(test_status)
--    Every dashboard query has: WHERE LOWER(pv.visit_status) != 'cancelled'
--    Without these, PostgreSQL cannot use a btree index and does a full scan.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_patient_visits_status_lower
    ON patient_visits (LOWER(visit_status));

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_visit_test_result_status_lower
    ON visit_test_result (LOWER(test_status));

-- 2. billing.created_at — used in every date-range filter on the billing table
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_billing_created_at
    ON billing (created_at);

-- 3. lab_billing join path — billing_id must be indexed for the JOIN
--    (the PK covers lab_id side; billing_id FK is the hot path)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_lab_billing_billing_id
    ON lab_billing (billing_id);

-- 4. labs.created_by — every super-admin query filters on this
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_labs_created_by
    ON labs (created_by);

-- 5. lab_visit.visit_id — join path for test/visit aggregations
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_lab_visit_visit_id
    ON lab_visit (visit_id);

-- 6. visit_test_result.visit_id + test_status compound index
--    Covers: WHERE vtr.visit_id = ? AND LOWER(vtr.test_status) = 'active'
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vtr_visit_id_status
    ON visit_test_result (visit_id, LOWER(test_status));

-- 7. visit_test_result.created_at — for date-range queries on tests
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vtr_created_at
    ON visit_test_result (created_at);

-- 8. patient_visits.created_at — date-range filter hot path
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_patient_visits_created_at
    ON patient_visits (created_at);

-- 9. patient_visits.billing_id — JOIN from billing to visit
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_patient_visits_billing_id
    ON patient_visits (billing_id);

-- 10. billing_transaction.billing_id — aggregation subquery grouping
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_billing_transaction_billing_id
    ON billing_transaction (billing_id);

-- Note: CONCURRENTLY means the index is built without locking writes.
-- Safe to run on a live production database.
-- Estimated time: seconds to a few minutes depending on table size.
