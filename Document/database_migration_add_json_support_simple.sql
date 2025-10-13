-- Simple migration script to add JSON support to super_admin_test_referance table
-- This script adds the Remarks and ReportJson columns to support JSON data storage

-- Add Remarks column
ALTER TABLE super_admin_test_referance 
ADD COLUMN IF NOT EXISTS remarks VARCHAR(500);

-- Add ReportJson column as TEXT first (for compatibility with Hibernate)
ALTER TABLE super_admin_test_referance 
ADD COLUMN IF NOT EXISTS report_json TEXT;

-- Create an index on the TEXT column for basic searching
CREATE INDEX IF NOT EXISTS idx_super_admin_test_referance_report_json_text 
ON super_admin_test_referance (report_json);

-- Add comments to document the new columns
COMMENT ON COLUMN super_admin_test_referance.remarks IS 'Additional remarks or notes for the test reference';
COMMENT ON COLUMN super_admin_test_referance.report_json IS 'JSON data containing structured report information (stored as TEXT for compatibility)';

-- Example of how to query JSON data (for reference):
-- SELECT * FROM super_admin_test_referance WHERE report_json LIKE '%USG_Carotid_Doppler%';
-- SELECT * FROM super_admin_test_referance WHERE report_json LIKE '%observations%';
-- SELECT * FROM super_admin_test_referance WHERE report_json LIKE '%opacity%';

-- Note: If you want to use JSONB for better performance, you can run this after the initial migration:
-- ALTER TABLE super_admin_test_referance ALTER COLUMN report_json TYPE JSONB USING report_json::JSONB;
-- CREATE INDEX IF NOT EXISTS idx_super_admin_test_referance_report_json_gin ON super_admin_test_referance USING GIN (report_json);

