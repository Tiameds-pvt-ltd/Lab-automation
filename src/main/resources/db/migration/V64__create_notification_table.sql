-- Notifications / approval requests (kept separate from every other module).
-- First use: PATIENT_EDIT_REQUEST. A lab user proposes a change to a patient's details; the proposed values are
-- stored here as JSON and nothing on the patient changes until an ADMIN of the same lab approves the request.
-- Other notification types can reuse this table later (type + entity_type + entity_id identify what it is about).
CREATE TABLE IF NOT EXISTS public.notification (
    id                      BIGSERIAL PRIMARY KEY,
    lab_id                  BIGINT                         NOT NULL REFERENCES public.labs (lab_id),

    -- e.g. PATIENT_EDIT_REQUEST
    type                    CHARACTER VARYING(50)          NOT NULL,
    -- what the notification is about, e.g. entity_type = PATIENT, entity_id = patient id
    entity_type             CHARACTER VARYING(50),
    entity_id               BIGINT,

    title                   CHARACTER VARYING(200)         NOT NULL,
    message                 TEXT,
    -- the proposed change (for PATIENT_EDIT_REQUEST: the PatientDTO the user submitted)
    payload_json            TEXT,

    requested_by_user_id    BIGINT,
    requested_by_username   CHARACTER VARYING(100),

    -- PENDING / APPROVED / REJECTED
    status                  CHARACTER VARYING(20)          NOT NULL DEFAULT 'PENDING',
    reviewed_by_user_id     BIGINT,
    reviewed_by_username    CHARACTER VARYING(100),
    reviewed_at             TIMESTAMP WITHOUT TIME ZONE,
    rejection_reason        TEXT,

    created_at              TIMESTAMP WITHOUT TIME ZONE,
    updated_at              TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_notification_lab_status
    ON public.notification (lab_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_requested_by
    ON public.notification (lab_id, requested_by_user_id, created_at DESC);

-- One open request per patient and type, so two pending edits can never overwrite each other
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_pending_entity
    ON public.notification (lab_id, type, entity_id) WHERE status = 'PENDING';
