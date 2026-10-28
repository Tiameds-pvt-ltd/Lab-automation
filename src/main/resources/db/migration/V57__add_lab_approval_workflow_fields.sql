-- Lab onboarding approval workflow: a newly onboarded lab waits for platform review before it becomes active.
-- Existing labs default to APPROVED so nothing changes for them.
ALTER TABLE public.labs
    ADD COLUMN IF NOT EXISTS approval_status       CHARACTER VARYING(20) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN IF NOT EXISTS approval_requested_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS reviewed_at           TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS reviewed_by           BIGINT,
    ADD COLUMN IF NOT EXISTS rejection_reason      TEXT;

CREATE INDEX IF NOT EXISTS idx_labs_approval_status ON public.labs (approval_status);
