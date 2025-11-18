-- Migration script for Sample Association Lab Isolation
-- This script adds lab_id to sample_entity table and migrates existing data
-- Date: 2024-01-15
-- Version: 1.0.0

-- Step 1: Add lab_id column (nullable initially for data migration)
ALTER TABLE sample_entity 
ADD COLUMN IF NOT EXISTS lab_id BIGINT;

-- Step 2: Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_sample_entity_lab_id ON sample_entity(lab_id);

-- Step 3: Backfill existing samples
-- IMPORTANT: Update this query based on your business logic
-- Option A: Assign all existing samples to a default lab (e.g., lab ID 1)
-- Uncomment and modify the lab_id value as needed:
UPDATE sample_entity 
SET lab_id = 1 
WHERE lab_id IS NULL;

-- Option B: If you have a mapping table or logic to determine lab_id for existing samples
-- UPDATE sample_entity se
-- SET lab_id = (
--     SELECT lab_id 
--     FROM your_mapping_table 
--     WHERE sample_id = se.sample_id
-- )
-- WHERE se.lab_id IS NULL;

-- Option C: If samples are associated with visits, you can derive lab_id from visits
-- UPDATE sample_entity se
-- SET lab_id = (
--     SELECT DISTINCT l.lab_id
--     FROM visit_sample vs
--     JOIN patient_visits pv ON vs.visit_id = pv.visit_id
--     JOIN lab_visit lv ON pv.visit_id = lv.visit_id
--     JOIN labs l ON lv.lab_id = l.lab_id
--     WHERE vs.sample_id = se.sample_id
--     LIMIT 1
-- )
-- WHERE se.lab_id IS NULL;

-- Step 4: Verify no NULL values remain
-- Run this query to check:
-- SELECT COUNT(*) as null_count 
-- FROM sample_entity 
-- WHERE lab_id IS NULL;
-- 
-- If null_count > 0, you need to handle those records before proceeding

-- Step 5: Make lab_id NOT NULL (only after all records have lab_id)
ALTER TABLE sample_entity 
ALTER COLUMN lab_id SET NOT NULL;

-- Step 6: Optional - Add composite unique constraint for name + lab_id
-- This ensures sample names are unique within a lab at the database level
-- Uncomment if you want database-level enforcement:
-- ALTER TABLE sample_entity 
-- ADD CONSTRAINT uk_sample_name_lab UNIQUE (name, lab_id);

-- Step 7: Add comment for documentation
COMMENT ON COLUMN sample_entity.lab_id IS 'Foreign key to labs table. Each sample belongs to a specific lab.';

-- Verification queries
-- Run these to verify the migration:

-- 1. Check all samples have lab_id
SELECT 
    COUNT(*) as total_samples,
    COUNT(lab_id) as samples_with_lab_id,
    COUNT(*) - COUNT(lab_id) as samples_without_lab_id
FROM sample_entity;

-- 2. Check distribution of samples across labs
SELECT 
    lab_id,
    COUNT(*) as sample_count
FROM sample_entity
GROUP BY lab_id
ORDER BY lab_id;

-- 3. Check for any duplicate sample names within the same lab
SELECT 
    lab_id,
    name,
    COUNT(*) as duplicate_count
FROM sample_entity
GROUP BY lab_id, name
HAVING COUNT(*) > 1;

-- If duplicates are found, you may want to rename them or handle them according to your business logic

-- Rollback script (use with caution - only if you need to revert)
-- ALTER TABLE sample_entity DROP COLUMN IF EXISTS lab_id;
-- DROP INDEX IF EXISTS idx_sample_entity_lab_id;
-- ALTER TABLE sample_entity DROP CONSTRAINT IF EXISTS uk_sample_name_lab;









