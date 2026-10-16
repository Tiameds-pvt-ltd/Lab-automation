ALTER TABLE public.patient_visit_tests
    ADD COLUMN IF NOT EXISTS test_name CHARACTER VARYING,
    ADD COLUMN IF NOT EXISTS price     NUMERIC;

-- Backfill existing rows from the tests master table, matched on test_id.
-- Only fills rows that are still NULL, so re-running never overwrites existing data.
UPDATE public.patient_visit_tests pvt
SET    test_name = t.name,
       price     = t.price
FROM   public.tests t
WHERE  pvt.test_id = t.test_id
  AND  (pvt.test_name IS NULL OR pvt.price IS NULL);
