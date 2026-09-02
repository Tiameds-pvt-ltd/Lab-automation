-- Migration: Create sequences for password reset tables
-- Required because ddl-auto: none in dev profile prevents Hibernate from auto-creating sequences
-- Run this once against the target database before starting the application

-- Sequence for password_reset_rate_limits.id
CREATE SEQUENCE IF NOT EXISTS pwd_reset_rate_limit_id_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Sequence for password_reset_tokens.id
CREATE SEQUENCE IF NOT EXISTS pwd_reset_token_id_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Ensure primary keys exist on both tables (safe to run if already present)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'password_reset_rate_limits' AND constraint_type = 'PRIMARY KEY'
    ) THEN
        ALTER TABLE password_reset_rate_limits ADD PRIMARY KEY (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'password_reset_tokens' AND constraint_type = 'PRIMARY KEY'
    ) THEN
        ALTER TABLE password_reset_tokens ADD PRIMARY KEY (id);
    END IF;
END $$;

-- Advance sequences past the current max IDs to avoid duplicate key errors on existing data
SELECT setval('pwd_reset_rate_limit_id_seq', (SELECT COALESCE(MAX(id), 0) FROM password_reset_rate_limits) + 1, false);
SELECT setval('pwd_reset_token_id_seq',      (SELECT COALESCE(MAX(id), 0) FROM password_reset_tokens) + 1, false);
