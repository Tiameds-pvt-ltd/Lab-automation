-- Migration script to add ReferenceRanges field to super_admin_test_referance table
-- This script adds the ReferenceRanges column to support JSON array storage of reference ranges

-- Add ReferenceRanges column as TEXT for compatibility
ALTER TABLE super_admin_test_referance 
ADD COLUMN IF NOT EXISTS reference_ranges TEXT;

-- Create an index on the TEXT column for basic searching
CREATE INDEX IF NOT EXISTS idx_super_admin_test_referance_reference_ranges_text 
ON super_admin_test_referance (reference_ranges);

-- Add comment to document the new column
COMMENT ON COLUMN super_admin_test_referance.reference_ranges IS 'JSON array containing multiple reference ranges with different age groups and gender specifications';

-- Example of how to query reference ranges data (for reference):
-- SELECT * FROM super_admin_test_referance WHERE reference_ranges LIKE '%MF%';
-- SELECT * FROM super_admin_test_referance WHERE reference_ranges LIKE '%MONTHS%';
-- SELECT * FROM super_admin_test_referance WHERE reference_ranges LIKE '%100 - 120%';

-- Note: If you want to use JSONB for better performance, you can run this after the initial migration:
-- ALTER TABLE super_admin_test_referance ALTER COLUMN reference_ranges TYPE JSONB USING reference_ranges::JSONB;
-- CREATE INDEX IF NOT EXISTS idx_super_admin_test_referance_reference_ranges_gin ON super_admin_test_referance USING GIN (reference_ranges);

