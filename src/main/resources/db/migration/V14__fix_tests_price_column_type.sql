-- Fix tests.price column created as character varying instead of NUMERIC.
-- Same root cause as V12/V13 (local dev DB schema drift).
-- USING clause safely converts existing string values to NUMERIC.

ALTER TABLE tests
    ALTER COLUMN price TYPE NUMERIC
        USING price::numeric;
