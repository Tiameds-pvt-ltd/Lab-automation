-- test_discount (TestDiscountEntity) has 3 BigDecimal columns never covered by
-- any prior migration: discount_amount, discount_percent, final_price.
-- Handles both "wrong type" and "column doesn't exist at all" per column
-- (see V25 for why both are possible).

DO $$
DECLARE
    col text;
BEGIN
    FOREACH col IN ARRAY ARRAY['discount_amount', 'discount_percent', 'final_price']
    LOOP
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'test_discount' AND column_name = col
        ) THEN
            EXECUTE format('ALTER TABLE test_discount ALTER COLUMN %I TYPE NUMERIC USING %I::numeric', col, col);
            RAISE NOTICE 'V27: fixed type of test_discount.%', col;
        ELSE
            EXECUTE format('ALTER TABLE test_discount ADD COLUMN %I NUMERIC', col);
            RAISE NOTICE 'V27: added missing column test_discount.%', col;
        END IF;
    END LOOP;
END $$;
