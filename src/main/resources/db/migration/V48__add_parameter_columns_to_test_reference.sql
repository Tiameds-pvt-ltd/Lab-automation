ALTER TABLE public.test_reference
    ADD COLUMN IF NOT EXISTS parameter_id   INTEGER,
    ADD COLUMN IF NOT EXISTS parameter_name CHARACTER VARYING;
