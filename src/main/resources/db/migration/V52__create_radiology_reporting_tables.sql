-- Radiology (imaging) reporting: templates, reports, signing doctors and an
-- append-only amendment history.
--
-- Purely additive: creates NEW tables only. It does not alter lab_report, tests,
-- doctors or any other existing table, so existing lab-report behaviour is unchanged.
--
-- No foreign keys point at pre-existing tables (patient_visits, tests, doctors, labs):
-- several of those had primary-key/identity drift fixed in V40, and a failing FK here
-- would block application startup. Ownership (lab / visit / test / doctor) is enforced
-- in RadiologyReportService instead. FKs between the NEW tables below are safe.

-- 1) Template: defines the form for one study type (e.g. Lower Limb Venous Doppler).
--    lab_id NULL = global template, lab_id set = lab-specific override.
--    A published template is never edited; a change creates a new version, so old
--    reports keep rendering exactly as they were signed.
CREATE TABLE IF NOT EXISTS radiology_template (
    id           BIGSERIAL PRIMARY KEY,
    test_id      BIGINT       NOT NULL,
    lab_id       BIGINT,
    name         VARCHAR(255) NOT NULL,
    version      INTEGER      NOT NULL DEFAULT 1,
    schema_json  JSONB        NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by   VARCHAR(255),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP WITH TIME ZONE
);

-- COALESCE so the uniqueness also applies to global templates (lab_id IS NULL).
CREATE UNIQUE INDEX IF NOT EXISTS uq_radiology_template_test_lab_version
    ON radiology_template (test_id, COALESCE(lab_id, 0), version);

CREATE INDEX IF NOT EXISTS idx_radiology_template_test_lab_active
    ON radiology_template (test_id, lab_id, is_active);

-- 2) Report: one row per (lab, visit, test).
CREATE TABLE IF NOT EXISTS radiology_report (
    id               BIGSERIAL PRIMARY KEY,
    report_code      VARCHAR(50)  NOT NULL,
    visit_id         BIGINT       NOT NULL,
    test_id          BIGINT       NOT NULL,
    lab_id           BIGINT       NOT NULL,
    template_id      BIGINT REFERENCES radiology_template (id),
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    clinical_history TEXT,
    technique        TEXT,
    comparison       TEXT,
    findings_json    JSONB        NOT NULL,
    impression       TEXT,
    finalized_at     TIMESTAMP WITH TIME ZONE,
    finalized_by     VARCHAR(255),
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_radiology_report_code UNIQUE (report_code),
    CONSTRAINT uq_radiology_report_lab_visit_test UNIQUE (lab_id, visit_id, test_id),
    CONSTRAINT chk_radiology_report_status CHECK (status IN ('DRAFT', 'FINAL', 'AMENDED'))
);

CREATE INDEX IF NOT EXISTS idx_radiology_report_lab_visit ON radiology_report (lab_id, visit_id);
CREATE INDEX IF NOT EXISTS idx_radiology_report_lab_status ON radiology_report (lab_id, status);

-- 3) Signing doctors. Name / qualification / licence are SNAPSHOTS taken at save time,
--    so a signed report does not change if the doctor's profile is edited later.
CREATE TABLE IF NOT EXISTS radiology_report_signatory (
    id              BIGSERIAL PRIMARY KEY,
    report_id       BIGINT       NOT NULL REFERENCES radiology_report (id) ON DELETE CASCADE,
    doctor_id       BIGINT       NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'REPORTING',
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    doctor_name     VARCHAR(255) NOT NULL,
    designation     VARCHAR(255),
    qualification   VARCHAR(255),
    license_number  VARCHAR(100),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_radiology_signatory_report_doctor UNIQUE (report_id, doctor_id),
    CONSTRAINT chk_radiology_signatory_role CHECK (role IN ('REPORTING', 'VERIFYING'))
);

CREATE INDEX IF NOT EXISTS idx_radiology_signatory_report ON radiology_report_signatory (report_id);

-- 4) Append-only history. A row is written with the PREVIOUS content every time a
--    FINAL/AMENDED report is changed. Never updated or deleted by the application.
CREATE TABLE IF NOT EXISTS radiology_report_history (
    id                BIGSERIAL PRIMARY KEY,
    report_id         BIGINT      NOT NULL REFERENCES radiology_report (id) ON DELETE CASCADE,
    report_version    BIGINT      NOT NULL,
    status            VARCHAR(20) NOT NULL,
    clinical_history  TEXT,
    technique         TEXT,
    comparison        TEXT,
    findings_json     JSONB,
    impression        TEXT,
    signatories_json  JSONB,
    change_reason     TEXT        NOT NULL,
    changed_by        VARCHAR(255),
    changed_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_radiology_history_report ON radiology_report_history (report_id, changed_at);
