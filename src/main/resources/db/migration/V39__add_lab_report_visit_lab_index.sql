-- lab_report has zero indexes today. Every super-admin dashboard query that
-- joins lab_report to compute avg turnaround time (avgTatHours) was doing a
-- full sequential scan of this table (~50k+ rows in the observed date range,
-- ~140ms of an ~460ms subquery, per EXPLAIN ANALYZE) plus a disk-spilled sort
-- on the resulting join. This index lets those joins use an index scan instead.

CREATE INDEX IF NOT EXISTS idx_lab_report_visit_id_lab_id ON lab_report(visit_id, lab_id);
