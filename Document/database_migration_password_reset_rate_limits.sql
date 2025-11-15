-- Migration script for password_reset_rate_limits table
-- This table stores rate limiting data for password reset requests
-- Allows shared rate limiting across multiple application instances

CREATE TABLE IF NOT EXISTS password_reset_rate_limits (
    id BIGSERIAL PRIMARY KEY,
    rate_limit_key VARCHAR(255) NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 1,
    window_start TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_pwd_reset_rate_limit_key ON password_reset_rate_limits(rate_limit_key);
CREATE INDEX IF NOT EXISTS idx_pwd_reset_rate_limit_expires ON password_reset_rate_limits(expires_at);

-- Composite index for common query pattern
CREATE INDEX IF NOT EXISTS idx_pwd_reset_rate_limit_key_expires 
    ON password_reset_rate_limits(rate_limit_key, expires_at);

-- Comments for documentation
COMMENT ON TABLE password_reset_rate_limits IS 'Stores rate limiting data for password reset requests. Shared across all application instances.';
COMMENT ON COLUMN password_reset_rate_limits.rate_limit_key IS 'Key format: "email:user@example.com" or "ip:127.0.0.1"';
COMMENT ON COLUMN password_reset_rate_limits.request_count IS 'Number of requests in current window';
COMMENT ON COLUMN password_reset_rate_limits.window_start IS 'Start time of current rate limit window';
COMMENT ON COLUMN password_reset_rate_limits.expires_at IS 'Expiration time - records are cleaned up after this';



