-- Certificate change requests: a lab user uploads a new / replacement certificate from the profile screen.
-- The file goes to S3 and a PENDING row is stored here; the lab keeps its current certificate until a platform
-- admin approves. Nothing is ever deleted from S3: the previous certificate stays as history, and so does the
-- upload of a rejected request.
CREATE TABLE IF NOT EXISTS public.lab_certificate_request (
    id                      BIGSERIAL PRIMARY KEY,
    lab_id                  BIGINT                         NOT NULL REFERENCES public.labs (lab_id),

    -- LabDocumentType name, e.g. NABL_ACCREDITATION_CERTIFICATE
    document_type           CHARACTER VARYING(60)          NOT NULL,
    requested_by_user_id    BIGINT,

    new_file_url            TEXT                           NOT NULL,
    -- The certificate that was live when the request was approved (history); null until then
    previous_file_url       TEXT,

    -- Only used for the establishment registration certificate (the only one with dates on labs)
    issuing_date            DATE,
    expiry_date             DATE,

    status                  CHARACTER VARYING(20)          NOT NULL DEFAULT 'PENDING',
    rejection_reason        TEXT,
    requested_at            TIMESTAMP WITHOUT TIME ZONE    NOT NULL,
    reviewed_at             TIMESTAMP WITHOUT TIME ZONE,

    created_at              TIMESTAMP WITHOUT TIME ZONE,
    updated_at              TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_lab_certificate_request_lab
    ON public.lab_certificate_request (lab_id, requested_at DESC);

CREATE INDEX IF NOT EXISTS idx_lab_certificate_request_status
    ON public.lab_certificate_request (status);

-- One open request per lab and certificate type
CREATE UNIQUE INDEX IF NOT EXISTS uq_lab_certificate_request_pending
    ON public.lab_certificate_request (lab_id, document_type) WHERE status = 'PENDING';
