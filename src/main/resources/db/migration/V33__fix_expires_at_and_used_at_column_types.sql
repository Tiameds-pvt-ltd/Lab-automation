-- Same drift pattern as V15/V22/V28/V29: these entities map expiresAt/usedAt
-- as java.time.Instant, but the columns were created as varchar, causing
-- Hibernate schema validation to fail on startup one table at a time.

ALTER TABLE otps
    ALTER COLUMN expires_at TYPE TIMESTAMP WITH TIME ZONE USING expires_at::timestamptz;

ALTER TABLE password_reset_rate_limits
    ALTER COLUMN expires_at TYPE TIMESTAMP WITH TIME ZONE USING expires_at::timestamptz;

ALTER TABLE password_reset_tokens
    ALTER COLUMN expires_at TYPE TIMESTAMP WITH TIME ZONE USING expires_at::timestamptz;

ALTER TABLE refresh_tokens
    ALTER COLUMN expires_at TYPE TIMESTAMP WITH TIME ZONE USING expires_at::timestamptz;

ALTER TABLE verification_tokens
    ALTER COLUMN used_at TYPE TIMESTAMP WITH TIME ZONE USING used_at::timestamptz;
