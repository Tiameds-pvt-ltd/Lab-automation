-- Migration script to add JSON support to super_admin_test_referance table
-- This script adds the Remarks and ReportJson columns to support JSON data storage

-- Add Remarks column
ALTER TABLE super_admin_test_referance 
ADD COLUMN IF NOT EXISTS remarks VARCHAR(500);

-- Add ReportJson column as TEXT first for compatibility
ALTER TABLE super_admin_test_referance 
ADD COLUMN IF NOT EXISTS report_json TEXT;

-- Convert TEXT column to JSONB for better performance and querying
-- This will automatically validate JSON data during conversion
ALTER TABLE super_admin_test_referance 
ALTER COLUMN report_json TYPE JSONB USING report_json::JSONB;

-- Create an index on the JSON column for better query performance
-- This is especially useful for searching within JSON content
CREATE INDEX IF NOT EXISTS idx_super_admin_test_referance_report_json 
ON super_admin_test_referance USING GIN (report_json);

-- Add comments to document the new columns
COMMENT ON COLUMN super_admin_test_referance.remarks IS 'Additional remarks or notes for the test reference';
COMMENT ON COLUMN super_admin_test_referance.report_json IS 'JSON data containing structured report information (e.g., observations, images, notes)';

-- Example of how to query JSON data (for reference):
-- SELECT * FROM super_admin_test_referance WHERE report_json ? 'USG_Carotid_Doppler';
-- SELECT * FROM super_admin_test_referance WHERE report_json->>'observations' LIKE '%opacity%';
-- SELECT * FROM super_admin_test_referance WHERE report_json @> '{"observations": "Lung opacity noted"}';
