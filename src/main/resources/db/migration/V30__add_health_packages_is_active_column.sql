-- HealthPackage entity declares is_active (boolean, not null, default true)
-- but the column was never added to health_packages, causing Hibernate
-- schema validation to fail on startup with "missing column [is_active]".

ALTER TABLE health_packages
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
