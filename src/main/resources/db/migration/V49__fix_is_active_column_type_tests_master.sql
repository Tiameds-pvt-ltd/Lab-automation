-- V49: Fix is_active column type in tbl_tests_master from SMALLINT to INTEGER
-- Hibernate maps Java int to INTEGER (Types#INTEGER); SMALLINT causes schema-validation failure.

ALTER TABLE tbl_tests_master
    ALTER COLUMN is_active TYPE INTEGER USING is_active::INTEGER;
