-- Onboarding: these lab fields are optional in /public/onboarding/complete, so they must accept NULL.
-- DROP NOT NULL is safe to re-run and does not touch existing data.
ALTER TABLE public.labs ALTER COLUMN certification_body DROP NOT NULL;
ALTER TABLE public.labs ALTER COLUMN lab_certificate DROP NOT NULL;
ALTER TABLE public.labs ALTER COLUMN director_govt_id DROP NOT NULL;
ALTER TABLE public.labs ALTER COLUMN lab_business_registration DROP NOT NULL;
ALTER TABLE public.labs ALTER COLUMN lab_license DROP NOT NULL;
ALTER TABLE public.labs ALTER COLUMN tax_id DROP NOT NULL;
