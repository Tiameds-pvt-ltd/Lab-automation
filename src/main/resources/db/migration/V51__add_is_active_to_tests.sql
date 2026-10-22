-- Soft delete support for tests: existing rows stay active, DELETE endpoint now flips this flag.
ALTER TABLE public.tests
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
