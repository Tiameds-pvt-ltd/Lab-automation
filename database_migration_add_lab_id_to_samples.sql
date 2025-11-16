-- Quick Migration: Add lab_id to sample_entity table
-- Run this script in your PostgreSQL database to fix the "column lab_id does not exist" error
-- Date: 2025-11-16

-- Step 1: Add lab_id column (nullable initially)
ALTER TABLE sample_entity 
ADD COLUMN IF NOT EXISTS lab_id BIGINT;

-- Step 2: Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_sample_entity_lab_id ON sample_entity(lab_id);

-- Step 3: Find the first available lab and assign all existing samples to it
-- This uses the first lab_id from the labs table, or defaults to 1 if no labs exist
DO $$
DECLARE
    first_lab_id BIGINT;
BEGIN
    -- Get the first lab_id from the labs table
    SELECT lab_id INTO first_lab_id 
    FROM labs 
    ORDER BY lab_id 
    LIMIT 1;
    
    -- If no labs exist, use lab_id = 1 (you may need to create a lab first)
    IF first_lab_id IS NULL THEN
        first_lab_id := 1;
        RAISE NOTICE 'No labs found in database. Using lab_id = 1. Please ensure lab with id 1 exists or update samples manually.';
    END IF;
    
    -- Update all existing samples to use this lab_id
    UPDATE sample_entity 
    SET lab_id = first_lab_id 
    WHERE lab_id IS NULL;
    
    RAISE NOTICE 'Updated all samples to use lab_id = %', first_lab_id;
END $$;

-- Step 4: Verify no NULL values remain
DO $$
DECLARE
    null_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO null_count 
    FROM sample_entity 
    WHERE lab_id IS NULL;
    
    IF null_count > 0 THEN
        RAISE EXCEPTION 'Migration failed: % samples still have NULL lab_id. Please assign lab_id manually before proceeding.', null_count;
    END IF;
    
    RAISE NOTICE 'Verification passed: All samples have lab_id assigned.';
END $$;

-- Step 5: Make lab_id NOT NULL (only after all records have lab_id)
ALTER TABLE sample_entity 
ALTER COLUMN lab_id SET NOT NULL;

-- Verification: Check the results
SELECT 
    'Migration completed successfully!' as status,
    COUNT(*) as total_samples,
    COUNT(DISTINCT lab_id) as unique_labs,
    MIN(lab_id) as min_lab_id,
    MAX(lab_id) as max_lab_id
FROM sample_entity;

-- Show distribution of samples across labs
SELECT 
    lab_id,
    COUNT(*) as sample_count
FROM sample_entity
GROUP BY lab_id
ORDER BY lab_id;

