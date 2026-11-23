-- Patient registration: date of birth is optional (age can be entered instead), so the column must accept NULL.
-- DROP NOT NULL is safe to re-run (no-op if already nullable) and does not touch existing data.
ALTER TABLE public.patients ALTER COLUMN date_of_birth DROP NOT NULL;
