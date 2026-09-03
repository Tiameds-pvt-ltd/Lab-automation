-- Same audit that found V25's gaps: V13 fixed only 3 of billing_transaction's
-- 6 BigDecimal columns (cash_amount, upi_amount, card_amount). received_amount,
-- refund_amount, due_amount were missed. Handles both "wrong type" and
-- "column doesn't exist at all" per column (see V25 for why both are possible).

DO $$
DECLARE
    col text;
BEGIN
    FOREACH col IN ARRAY ARRAY['received_amount', 'refund_amount', 'due_amount']
    LOOP
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'billing_transaction' AND column_name = col
        ) THEN
            EXECUTE format('ALTER TABLE billing_transaction ALTER COLUMN %I TYPE NUMERIC USING %I::numeric', col, col);
            RAISE NOTICE 'V26: fixed type of billing_transaction.%', col;
        ELSE
            EXECUTE format('ALTER TABLE billing_transaction ADD COLUMN %I NUMERIC', col);
            RAISE NOTICE 'V26: added missing column billing_transaction.%', col;
        END IF;
    END LOOP;
END $$;
