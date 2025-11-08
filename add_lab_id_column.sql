-- Migration script to add lab_id column to lab_audit_logs table
-- This column is required for tracking which lab performed the audit action

ALTER TABLE lab_audit_logs 
ADD COLUMN IF NOT EXISTS lab_id VARCHAR(255) NOT NULL DEFAULT '';

-- If you need to backfill existing records, you may need to:
-- 1. First, make the column nullable temporarily
-- 2. Update existing records with appropriate lab_id values
-- 3. Then make it NOT NULL again

-- For new installations, you can remove the DEFAULT clause after the column is added:
-- ALTER TABLE lab_audit_logs ALTER COLUMN lab_id DROP DEFAULT;

