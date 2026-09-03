-- Same drift as V15/V22 (created_at columns created as character varying instead
-- of TIMESTAMP WITH TIME ZONE on local dev DB): billing_transaction.created_at
-- was never fixed, so Hibernate schema validation fails on startup expecting
-- timestamp(6) with time zone but finding varchar. USING clause safely converts
-- existing data.

ALTER TABLE billing_transaction
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE
        USING created_at::timestamptz;
