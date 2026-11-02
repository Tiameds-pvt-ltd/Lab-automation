-- add-lab (an existing, logged-in user creating another lab) now goes through the same platform approval as
-- onboarding: the request is stored in temp_lab and the lab is created in labs only when it is approved.
--
-- existing_user_id : set for add-lab requests (the user who asked for the lab); NULL for onboarding requests.
--                    On approval no user is created, the lab is created for this user instead.
-- Applicant columns (password, names, address...) are only meaningful for onboarding, so they become nullable.
ALTER TABLE public.temp_lab ADD COLUMN IF NOT EXISTS existing_user_id BIGINT;

ALTER TABLE public.temp_lab ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE public.temp_lab ALTER COLUMN first_name    DROP NOT NULL;
ALTER TABLE public.temp_lab ALTER COLUMN last_name     DROP NOT NULL;
ALTER TABLE public.temp_lab ALTER COLUMN phone         DROP NOT NULL;
ALTER TABLE public.temp_lab ALTER COLUMN address       DROP NOT NULL;
ALTER TABLE public.temp_lab ALTER COLUMN city          DROP NOT NULL;
ALTER TABLE public.temp_lab ALTER COLUMN state         DROP NOT NULL;
ALTER TABLE public.temp_lab ALTER COLUMN zip           DROP NOT NULL;
ALTER TABLE public.temp_lab ALTER COLUMN country       DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_temp_lab_existing_user ON public.temp_lab (existing_user_id);

-- The "one open request per applicant" guard only applies to onboarding requests (new accounts).
-- An existing user is not blocked from having a pending lab request by their own email / username.
DROP INDEX IF EXISTS public.uq_temp_lab_pending_email;
DROP INDEX IF EXISTS public.uq_temp_lab_pending_username;
CREATE UNIQUE INDEX IF NOT EXISTS uq_temp_lab_pending_email
    ON public.temp_lab (LOWER(email)) WHERE approval_status = 'PENDING' AND existing_user_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_temp_lab_pending_username
    ON public.temp_lab (username) WHERE approval_status = 'PENDING' AND existing_user_id IS NULL;
