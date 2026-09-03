-- One-time correction of a specific user's email address.
-- Guarded: only proceeds if exactly one row matches the old email and the new
-- email isn't already taken by someone else — otherwise it raises and does
-- nothing, rather than risking an unscoped UPDATE across all users.
-- Safe to ship to every environment: if the old email doesn't exist here
-- (e.g. a fresh dev/test DB), it's a no-op.

DO $$
DECLARE
    match_count integer;
    conflict_count integer;
BEGIN
    SELECT COUNT(*) INTO match_count FROM public.users WHERE email = 'somilm@tiameds.ai';
    SELECT COUNT(*) INTO conflict_count FROM public.users WHERE email = 'dipakdagadu@tiameds.ai';

    IF match_count = 0 THEN
        RAISE NOTICE 'V18: no user with email somilm@tiameds.ai found — skipping (already applied or not present in this environment)';
    ELSIF match_count > 1 THEN
        RAISE EXCEPTION 'V18: expected exactly 1 user with email somilm@tiameds.ai, found %. Aborting to avoid an unscoped update.', match_count;
    ELSIF conflict_count > 0 THEN
        RAISE EXCEPTION 'V18: target email dipakdagadu@tiameds.ai is already in use by % row(s). Aborting.', conflict_count;
    ELSE
        UPDATE public.users SET email = 'dipakdagadu@tiameds.ai' WHERE email = 'somilm@tiameds.ai';
        RAISE NOTICE 'V18: updated 1 user email somilm@tiameds.ai -> dipakdagadu@tiameds.ai';
    END IF;
END $$;
