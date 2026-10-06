-- V48: Add auto-increment sequence and is_active flag for tbl_tests_master
-- Seed data in V47 uses explicit IDs up to ~637; sequence starts at 10000 to avoid conflicts.
-- is_active: 1 = active, 0 = inactive (soft delete)

CREATE SEQUENCE IF NOT EXISTS tbl_tests_master_test_id_seq START WITH 10000 INCREMENT BY 1;

ALTER TABLE tbl_tests_master
    ALTER COLUMN test_id SET DEFAULT nextval('tbl_tests_master_test_id_seq');

ALTER TABLE tbl_tests_master
    ADD COLUMN IF NOT EXISTS is_active SMALLINT NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1));
