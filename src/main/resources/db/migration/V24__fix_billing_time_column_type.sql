-- Same drift as V12/V15/V21/V22: billing.billing_time was created as
-- character varying instead of TIME, which is what BillingEntity.billingTime
-- (LocalTime) actually maps to. USING clause safely converts existing text
-- values (e.g. "14:35:00") to a real TIME(6) column.

ALTER TABLE billing
    ALTER COLUMN billing_time TYPE TIME(6)
        USING billing_time::time(6);
