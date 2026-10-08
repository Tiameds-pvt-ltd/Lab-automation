-- V50: Add parameter_id and parameter_name to test_reference for UI dropdown linkage

ALTER TABLE public.test_reference
    ADD COLUMN IF NOT EXISTS parameter_id   INTEGER,
    ADD COLUMN IF NOT EXISTS parameter_name CHARACTER VARYING;

ALTER TABLE public.test_reference
    ADD CONSTRAINT fk_test_reference_parameter
        FOREIGN KEY (parameter_id)
            REFERENCES public.tbl_parameter_master (parameter_id)
            ON UPDATE CASCADE
            ON DELETE SET NULL;
