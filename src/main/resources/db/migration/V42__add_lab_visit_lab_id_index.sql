-- lab_visit(lab_id) is used in every per-lab earnings query (WHERE lv.lab_id = :labId or IN (:labIds)).
-- Without this index PostgreSQL does a full scan of lab_visit to find visits for a given lab.
CREATE INDEX IF NOT EXISTS idx_lab_visit_lab_id ON lab_visit (lab_id);
