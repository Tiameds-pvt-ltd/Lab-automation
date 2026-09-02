-- Fix billing_transaction money columns created as character varying instead of NUMERIC.
-- Same root cause as V12 (local dev DB schema drift).
-- USING clause safely converts existing string values to NUMERIC.

ALTER TABLE billing_transaction
    ALTER COLUMN cash_amount TYPE NUMERIC
        USING cash_amount::numeric,
    ALTER COLUMN upi_amount TYPE NUMERIC
        USING upi_amount::numeric,
    ALTER COLUMN card_amount TYPE NUMERIC
        USING card_amount::numeric;
