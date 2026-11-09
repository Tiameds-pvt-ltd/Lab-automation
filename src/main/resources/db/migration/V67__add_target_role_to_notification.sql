-- Who an information-only alert is meant for. NULL = the lab's admins (as before).
-- 'SUPERADMIN' = only super admins (e.g. an ADMIN changed a test / test reference).
ALTER TABLE public.notification ADD COLUMN IF NOT EXISTS target_role CHARACTER VARYING(20);

CREATE INDEX IF NOT EXISTS idx_notification_target_role
    ON public.notification (target_role, status, created_at DESC);
