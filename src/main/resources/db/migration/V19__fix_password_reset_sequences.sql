-- password_reset_rate_limits.id / password_reset_tokens.id were created (via
-- ddl-auto=update drift, same root cause as V12-V16) without a sequence wired
-- as their DEFAULT, so inserts relying on auto-generated ids fail. This wires
-- the missing sequence + default and advances it past any already-restored data
-- to avoid duplicate-key errors on the next insert.
-- Guarded with to_regclass so this is a no-op if either table doesn't exist yet
-- in a given environment.

DO $$
BEGIN
    IF to_regclass('public.password_reset_rate_limits') IS NOT NULL THEN
        CREATE SEQUENCE IF NOT EXISTS pwd_reset_rate_limit_id_seq
            START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

        ALTER TABLE password_reset_rate_limits
            ALTER COLUMN id SET DEFAULT nextval('pwd_reset_rate_limit_id_seq');

        PERFORM setval('pwd_reset_rate_limit_id_seq',
            (SELECT COALESCE(MAX(id), 0) FROM password_reset_rate_limits) + 1, false);
    END IF;

    IF to_regclass('public.password_reset_tokens') IS NOT NULL THEN
        CREATE SEQUENCE IF NOT EXISTS pwd_reset_token_id_seq
            START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

        ALTER TABLE password_reset_tokens
            ALTER COLUMN id SET DEFAULT nextval('pwd_reset_token_id_seq');

        PERFORM setval('pwd_reset_token_id_seq',
            (SELECT COALESCE(MAX(id), 0) FROM password_reset_tokens) + 1, false);
    END IF;
END $$;
