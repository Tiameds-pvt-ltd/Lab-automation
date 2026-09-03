-- Same drift pattern as V12/V13/V14 (billing/billing_transaction/tests numeric columns):
-- billing.actual_received_amount needs to be NUMERIC(15,2) to match BillingEntity's
-- BigDecimal mapping. The USING clause tolerates NULL/empty-string values already
-- present from prior drift; any value that doesn't actually fit NUMERIC(15,2) will
-- fail this migration loudly rather than being silently truncated.

ALTER TABLE billing
    ALTER COLUMN actual_received_amount TYPE NUMERIC(15, 2)
    USING CASE
        WHEN actual_received_amount IS NULL OR actual_received_amount::text = '' THEN NULL
        ELSE actual_received_amount::NUMERIC(15, 2)
    END;
