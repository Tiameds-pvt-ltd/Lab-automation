-- Test script to check gender constraint and fix if needed
-- This script will help identify and fix the gender constraint issue

-- First, let's check what the current constraint allows
SELECT conname, consrc 
FROM pg_constraint 
WHERE conrelid = 'super_admin_test_referance'::regclass 
AND conname LIKE '%gender%';

-- If the constraint is too restrictive, we can drop and recreate it
-- DROP CONSTRAINT IF EXISTS super_admin_test_referance_gender_check;

-- Add a more permissive constraint that allows M, F, and M/F
-- ALTER TABLE super_admin_test_referance 
-- ADD CONSTRAINT super_admin_test_referance_gender_check 
-- CHECK (gender IN ('M', 'F', 'M/F'));

-- Or if we want to allow NULL as well:
-- ALTER TABLE super_admin_test_referance 
-- ADD CONSTRAINT super_admin_test_referance_gender_check 
-- CHECK (gender IS NULL OR gender IN ('M', 'F', 'M/F'));

-- Test insert to verify the constraint works
-- INSERT INTO super_admin_test_referance (category, test_name, gender, created_by, updated_by) 
-- VALUES ('TEST', 'Test Name', 'M/F', 'test', 'test');

