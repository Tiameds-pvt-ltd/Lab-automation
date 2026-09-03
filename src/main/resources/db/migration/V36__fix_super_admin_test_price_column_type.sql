-- SuperAdminTestEntity maps price as BigDecimal (numeric(38,2)), but the
-- column was created as varchar, causing Hibernate schema validation to
-- fail on startup.

ALTER TABLE super_admin_test_pricelistentity
    ALTER COLUMN price TYPE NUMERIC(38,2) USING price::numeric(38,2);
