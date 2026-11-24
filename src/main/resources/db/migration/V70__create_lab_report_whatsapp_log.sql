-- Delivery log for lab reports sent to patients over WhatsApp (AWS End User Messaging Social).
-- One row per send attempt. The PDF itself lives in a PRIVATE S3 bucket; only the object key is stored here
-- (never a public URL).
-- No foreign keys on visit_id / patient_id on purpose: visits can be deleted, and the delivery history must
-- not block that (or be cascaded away).
CREATE TABLE IF NOT EXISTS public.lab_report_whatsapp_log (
    id                      BIGSERIAL PRIMARY KEY,
    lab_id                  BIGINT                         NOT NULL,
    visit_id                BIGINT                         NOT NULL,
    patient_id              BIGINT                         NOT NULL,

    -- recipient in E.164 format, e.g. +919876543210
    phone_number            CHARACTER VARYING(20)          NOT NULL,
    template_name           CHARACTER VARYING(100)         NOT NULL,

    -- S3 object key of the PDF that was sent (private bucket)
    s3_key                  CHARACTER VARYING(512),
    -- media id returned by PostWhatsAppMessageMedia
    media_id                CHARACTER VARYING(100),
    -- message id returned by SendWhatsAppMessage (wamid...)
    whatsapp_message_id     CHARACTER VARYING(255),

    -- PENDING / SENT / FAILED (DELIVERED / READ reserved for webhook status updates)
    status                  CHARACTER VARYING(20)          NOT NULL DEFAULT 'PENDING',
    error_message           CHARACTER VARYING(1000),

    sent_by_user_id         BIGINT,
    sent_by_username        CHARACTER VARYING(100),

    created_at              TIMESTAMP WITHOUT TIME ZONE,
    sent_at                 TIMESTAMP WITHOUT TIME ZONE,
    delivered_at            TIMESTAMP WITHOUT TIME ZONE,
    updated_at              TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_lab_report_wa_visit
    ON public.lab_report_whatsapp_log (lab_id, visit_id, created_at DESC);

-- used to attach delivery receipts (webhook) to the right row
CREATE INDEX IF NOT EXISTS idx_lab_report_wa_message_id
    ON public.lab_report_whatsapp_log (whatsapp_message_id);
