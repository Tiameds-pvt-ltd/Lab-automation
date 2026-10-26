-- Word report formats (.doc / .docx) uploaded from the radiology report screen.
CREATE TABLE IF NOT EXISTS radiology_report_format (
    id             BIGSERIAL PRIMARY KEY,
    lab_id         BIGINT       NOT NULL,
    document_name  VARCHAR(255) NOT NULL,
    content_type   VARCHAR(150),
    file_size      BIGINT       NOT NULL,
    file_data      BYTEA        NOT NULL,
    doctor_id      BIGINT,
    doctor_name    VARCHAR(255) NOT NULL,
    uploaded_by    VARCHAR(255),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_radiology_report_format_lab ON radiology_report_format (lab_id);
