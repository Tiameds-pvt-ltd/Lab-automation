-- Migration script to add timestamps and code to patient_visit_sample table
-- This converts the simple join table to an entity table with timestamps and codes

-- Step 1: Add new columns to patient_visit_sample table
ALTER TABLE patient_visit_sample 
ADD COLUMN IF NOT EXISTS id BIGSERIAL PRIMARY KEY,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS visit_sample_code VARCHAR(255) UNIQUE;

-- Step 2: Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_patient_visit_sample_visit_id 
ON patient_visit_sample(visit_id);

CREATE INDEX IF NOT EXISTS idx_patient_visit_sample_sample_id 
ON patient_visit_sample(sample_id);

CREATE INDEX IF NOT EXISTS idx_patient_visit_sample_code 
ON patient_visit_sample(visit_sample_code);

-- Step 3: Add comments to document the new columns
COMMENT ON TABLE patient_visit_sample IS 'Intermediate entity table for visit-sample relationships with timestamps and codes';
COMMENT ON COLUMN patient_visit_sample.id IS 'Primary key for the visit-sample relationship';
COMMENT ON COLUMN patient_visit_sample.visit_id IS 'Foreign key to patient_visits table';
COMMENT ON COLUMN patient_visit_sample.sample_id IS 'Foreign key to sample_entity table';
COMMENT ON COLUMN patient_visit_sample.created_at IS 'Timestamp when the sample was associated with the visit';
COMMENT ON COLUMN patient_visit_sample.updated_at IS 'Timestamp when the relationship was last updated';
COMMENT ON COLUMN patient_visit_sample.created_by IS 'Username who created the association';
COMMENT ON COLUMN patient_visit_sample.updated_by IS 'Username who last updated the association';
COMMENT ON COLUMN patient_visit_sample.visit_sample_code IS 'Unique code for this visit-sample relationship (e.g., VTR1-00001)';

-- Note: If there are existing records in patient_visit_sample, you may want to:
-- 1. Set created_at to a default timestamp for existing records
-- 2. Generate codes for existing records if needed
-- Example:
-- UPDATE patient_visit_sample SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
-- UPDATE patient_visit_sample SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

