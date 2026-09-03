-- AiClinicalObservation.java maps to this table but it was never created in this
-- DB — a genuinely missing table, not a type-drift issue like V12-V22. It only
-- surfaced now because ddl-auto was switched from none to validate, which checks
-- every entity's table actually exists at startup. Purely additive: creates a
-- new table only, does not alter anything existing.

CREATE TABLE IF NOT EXISTS ai_clinical_observations (
    id                       BIGSERIAL PRIMARY KEY,
    visit_id                 BIGINT NOT NULL,
    provisional_diagnosis    TEXT,
    clinical_interpretation  TEXT,
    doctor_to_visit          VARCHAR(255),
    patient_interpretation   TEXT,
    tips                     TEXT,
    content_hash             VARCHAR(64),
    created_by               VARCHAR(255),
    updated_by               VARCHAR(255),
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_ai_obs_visit UNIQUE (visit_id)
);
