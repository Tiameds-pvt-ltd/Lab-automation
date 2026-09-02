-- Fix billing money columns that were created as character varying instead of NUMERIC.
-- This only affects local dev DBs where ddl-auto was 'none' when these columns were added.
-- Production already has NUMERIC via Hibernate auto-migration, so ALTER is a no-op there
-- (changing NUMERIC to NUMERIC does nothing).
-- USING clause handles safe conversion of any existing varchar data.

ALTER TABLE billing
    ALTER COLUMN actual_received_amount TYPE NUMERIC
        USING actual_received_amount::numeric,
    ALTER COLUMN due_amount TYPE NUMERIC
        USING due_amount::numeric,
    ALTER COLUMN discount TYPE NUMERIC
        USING discount::numeric,
    ALTER COLUMN total_amount TYPE NUMERIC
        USING total_amount::numeric,
    ALTER COLUMN net_amount TYPE NUMERIC
        USING net_amount::numeric,
    ALTER COLUMN gst_amount TYPE NUMERIC
        USING gst_amount::numeric,
    ALTER COLUMN cgst_amount TYPE NUMERIC
        USING cgst_amount::numeric,
    ALTER COLUMN sgst_amount TYPE NUMERIC
        USING sgst_amount::numeric,
    ALTER COLUMN igst_amount TYPE NUMERIC
        USING igst_amount::numeric;
