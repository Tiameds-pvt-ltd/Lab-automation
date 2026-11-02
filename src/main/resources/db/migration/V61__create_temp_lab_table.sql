-- Onboarding staging table: /public/onboarding/complete stores the whole submission here.
-- Only when a platform admin approves it are the user and the lab created in users / labs.
-- A rejected request stays here (status REJECTED) and never reaches users / labs.
-- The password is stored as a BCrypt hash, never in plain text.
CREATE TABLE IF NOT EXISTS public.temp_lab (
    id                                      BIGSERIAL PRIMARY KEY,

    -- Applicant (becomes the SUPERADMIN user on approval)
    email                                   CHARACTER VARYING(255) NOT NULL,
    username                                CHARACTER VARYING(50)  NOT NULL,
    password_hash                           CHARACTER VARYING(255) NOT NULL,
    first_name                              CHARACTER VARYING(255) NOT NULL,
    last_name                               CHARACTER VARYING(255) NOT NULL,
    phone                                   CHARACTER VARYING(255) NOT NULL,
    address                                 CHARACTER VARYING(255) NOT NULL,
    city                                    CHARACTER VARYING(255) NOT NULL,
    state                                   CHARACTER VARYING(255) NOT NULL,
    zip                                     CHARACTER VARYING(255) NOT NULL,
    country                                 CHARACTER VARYING(255) NOT NULL,

    -- Organization
    organization_name                       CHARACTER VARYING(100),
    organization_type                       CHARACTER VARYING(50),
    organization_email                      CHARACTER VARYING(255),
    organization_phone                      CHARACTER VARYING(20),
    organization_address_line1              CHARACTER VARYING(100),
    organization_address_line2              CHARACTER VARYING(100),
    organization_country                    CHARACTER VARYING(100),
    organization_state                      CHARACTER VARYING(100),
    organization_district                   CHARACTER VARYING(100),
    organization_city                       CHARACTER VARYING(50),
    organization_pin_code                   CHARACTER VARYING(10),
    organization_pan                        CHARACTER VARYING(10),
    organization_gstin                      CHARACTER VARYING(15),
    organization_gstin_certificate          CHARACTER VARYING,

    -- Lab
    lab_name                                CHARACTER VARYING(255) NOT NULL,
    lab_address                             CHARACTER VARYING(255) NOT NULL,
    lab_city                                CHARACTER VARYING(255) NOT NULL,
    lab_state                               CHARACTER VARYING(255) NOT NULL,
    lab_description                         CHARACTER VARYING(255) NOT NULL,
    lab_logo                                CHARACTER VARYING,
    license_number                          CHARACTER VARYING(255) NOT NULL,
    lab_type                                CHARACTER VARYING(255) NOT NULL,
    lab_zip                                 CHARACTER VARYING(255) NOT NULL,
    lab_country                             CHARACTER VARYING(255) NOT NULL,
    lab_phone                               CHARACTER VARYING(255) NOT NULL,
    lab_email                               CHARACTER VARYING(255) NOT NULL,
    director_name                           CHARACTER VARYING(255) NOT NULL,
    director_email                          CHARACTER VARYING(255) NOT NULL,
    director_phone                          CHARACTER VARYING(255) NOT NULL,
    certification_body                      CHARACTER VARYING(255),
    lab_certificate                         CHARACTER VARYING(255),
    director_govt_id                        CHARACTER VARYING(255),
    lab_business_registration               CHARACTER VARYING(255),
    lab_license                             CHARACTER VARYING(255),
    tax_id                                  CHARACTER VARYING(255),
    lab_accreditation                       CHARACTER VARYING(255) NOT NULL,
    data_privacy_agreement                  BOOLEAN NOT NULL,
    lab_services                            TEXT,
    same_as_organization_address            BOOLEAN,
    lab_address_line2                       CHARACTER VARYING(100),
    lab_district                            CHARACTER VARYING(100),

    -- Compliance
    establishment_registration_number       CHARACTER VARYING(50),
    issuing_authority                       CHARACTER VARYING(25),
    issuing_date                            DATE,
    expiry_date                             DATE,
    establishment_registration_certificate  CHARACTER VARYING,
    lab_pan                                 CHARACTER VARYING(10),
    lab_pan_same_as_organization            BOOLEAN,
    lab_gstin                               CHARACTER VARYING(15),
    lab_gstin_same_as_organization          BOOLEAN,
    gstin_certificate                       CHARACTER VARYING,
    biomedical_waste_generated              BOOLEAN,
    biomedical_waste_authorization_number   CHARACTER VARYING(25),
    biomedical_waste_authorization_certificate CHARACTER VARYING,
    nabl_accreditation_certificate          CHARACTER VARYING,

    -- Authorization and Terms & Conditions
    registration_authorization_accepted     BOOLEAN,
    terms_accepted                          BOOLEAN,
    terms_version                           CHARACTER VARYING(20),
    terms_accepted_at                       TIMESTAMP WITHOUT TIME ZONE,

    -- Review
    approval_status                         CHARACTER VARYING(20) NOT NULL DEFAULT 'PENDING',
    approval_requested_at                   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    reviewed_at                             TIMESTAMP WITHOUT TIME ZONE,
    rejection_reason                        TEXT,
    approved_user_id                        BIGINT,
    approved_lab_id                         BIGINT,

    created_at                              TIMESTAMP WITHOUT TIME ZONE,
    updated_at                              TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_temp_lab_approval_status ON public.temp_lab (approval_status);

-- One open request per applicant: also guards against two simultaneous submissions.
-- Rejected / approved rows do not block a new request.
CREATE UNIQUE INDEX IF NOT EXISTS uq_temp_lab_pending_email
    ON public.temp_lab (LOWER(email)) WHERE approval_status = 'PENDING';
CREATE UNIQUE INDEX IF NOT EXISTS uq_temp_lab_pending_username
    ON public.temp_lab (username) WHERE approval_status = 'PENDING';
