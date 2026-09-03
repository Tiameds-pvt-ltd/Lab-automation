-- Fix refresh_tokens.id column created as character varying instead of UUID.
-- Same root cause as V12/V13/V14 (local dev DB schema drift under ddl-auto=update).
-- USING clause safely converts existing string values to UUID.

ALTER TABLE refresh_tokens
    ALTER COLUMN id TYPE uuid
        USING id::uuid;
