-- Notifications about a visit (e.g. patient edit with visit / billing details) carry the visit they belong to,
-- so the admin screen can show the visit code without opening the request.
ALTER TABLE public.notification ADD COLUMN IF NOT EXISTS visit_id   BIGINT;
ALTER TABLE public.notification ADD COLUMN IF NOT EXISTS visit_code CHARACTER VARYING(100);
