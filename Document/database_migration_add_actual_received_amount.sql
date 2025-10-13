-- Migration script to add actual_received_amount field to billing table
-- This field will track the actual amount received from patients (before any refunds)

-- Add the new column to the billing table
ALTER TABLE billing 
ADD COLUMN actual_received_amount DECIMAL(19,2) DEFAULT 0.00;

-- Update existing records to set actual_received_amount = received_amount
-- This ensures data consistency for existing records
UPDATE billing 
SET actual_received_amount = COALESCE(received_amount, 0.00)
WHERE actual_received_amount IS NULL;

-- Add a comment to document the field purpose
COMMENT ON COLUMN billing.actual_received_amount IS 'Actual amount received from patient (before refunds). This is the gross amount paid by the patient.';

-- Optional: Add an index for better query performance if needed
-- CREATE INDEX idx_billing_actual_received_amount ON billing(actual_received_amount);

