-- Migration script to create lab_entity_sequence table
-- This table stores the last sequence number for each entity type per lab
-- Supports multi-tenant sequence generation with transaction-safe concurrent access

-- Create the lab_entity_sequence table
CREATE TABLE IF NOT EXISTS lab_entity_sequence (
    lab_id BIGINT NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    last_number BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (lab_id, entity_name),
    CONSTRAINT fk_lab_entity_sequence_lab FOREIGN KEY (lab_id) REFERENCES labs(lab_id) ON DELETE CASCADE
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_lab_entity_sequence_lab_entity 
ON lab_entity_sequence(lab_id, entity_name);

-- Add comments to document the table
COMMENT ON TABLE lab_entity_sequence IS 'Stores the last sequence number for each entity type per lab. Used for generating unique, sequential codes like PAT-00001, VIS-00001, etc.';
COMMENT ON COLUMN lab_entity_sequence.lab_id IS 'Foreign key to labs table';
COMMENT ON COLUMN lab_entity_sequence.entity_name IS 'Name of the entity type (e.g., PATIENT, VISIT, BILLING)';
COMMENT ON COLUMN lab_entity_sequence.last_number IS 'Last sequence number used for this entity type in this lab';
COMMENT ON COLUMN lab_entity_sequence.created_at IS 'Timestamp when the record was created';
COMMENT ON COLUMN lab_entity_sequence.updated_at IS 'Timestamp when the record was last updated';

