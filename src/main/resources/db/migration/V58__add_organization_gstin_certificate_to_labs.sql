-- Onboarding: the organization and the lab each have their own GST certificate.
-- The existing gstin_certificate column keeps holding the lab's certificate; this adds the organization's.
ALTER TABLE public.labs
    ADD COLUMN IF NOT EXISTS organization_gstin_certificate CHARACTER VARYING;
