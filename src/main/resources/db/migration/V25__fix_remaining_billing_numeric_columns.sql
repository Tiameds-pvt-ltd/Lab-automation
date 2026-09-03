-- V12 fixed 9 of billing's 13 BigDecimal columns. Of the 4 remaining
-- (gst_rate, received_amount, package_amt, package_discount), some are wrong-typed
-- (varchar instead of numeric) and some don't exist in this DB at all
-- (package_amt/package_discount are newer fields added to BillingEntity after
-- this dev DB's schema was last touched). Handle both cases per column instead
-- of assuming which case applies, so this migration can't fail on either.

DO $$
DECLARE
    col text;
BEGIN
    FOREACH col IN ARRAY ARRAY['gst_rate', 'received_amount', 'package_amt', 'package_discount']
    LOOP
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'billing' AND column_name = col
        ) THEN
            EXECUTE format('ALTER TABLE billing ALTER COLUMN %I TYPE NUMERIC USING %I::numeric', col, col);
            RAISE NOTICE 'V25: fixed type of billing.%', col;
        ELSE
            EXECUTE format('ALTER TABLE billing ADD COLUMN %I NUMERIC', col);
            RAISE NOTICE 'V25: added missing column billing.%', col;
        END IF;
    END LOOP;
END $$;
