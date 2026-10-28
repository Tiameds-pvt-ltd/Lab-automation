-- Lab Onboarding Process: organization info, extra lab info, compliance details and T&C acceptance.
-- All columns are nullable so existing labs and existing onboarding clients keep working unchanged.
ALTER TABLE public.labs
    -- Step 2: Organization information
    ADD COLUMN IF NOT EXISTS organization_name            CHARACTER VARYING(100),
    ADD COLUMN IF NOT EXISTS organization_type            CHARACTER VARYING(50),
    ADD COLUMN IF NOT EXISTS organization_email           CHARACTER VARYING(255),
    ADD COLUMN IF NOT EXISTS organization_phone           CHARACTER VARYING(20),
    ADD COLUMN IF NOT EXISTS organization_address_line1   CHARACTER VARYING(100),
    ADD COLUMN IF NOT EXISTS organization_address_line2   CHARACTER VARYING(100),
    ADD COLUMN IF NOT EXISTS organization_country         CHARACTER VARYING(100),
    ADD COLUMN IF NOT EXISTS organization_state           CHARACTER VARYING(100),
    ADD COLUMN IF NOT EXISTS organization_district        CHARACTER VARYING(100),
    ADD COLUMN IF NOT EXISTS organization_city           CHARACTER VARYING(50),
    ADD COLUMN IF NOT EXISTS organization_pin_code        CHARACTER VARYING(10),
    ADD COLUMN IF NOT EXISTS organization_pan             CHARACTER VARYING(10),
    ADD COLUMN IF NOT EXISTS organization_gstin           CHARACTER VARYING(15),

    -- Step 3: First laboratory information
    ADD COLUMN IF NOT EXISTS lab_services                 TEXT,
    ADD COLUMN IF NOT EXISTS same_as_organization_address BOOLEAN,
    ADD COLUMN IF NOT EXISTS lab_address_line2            CHARACTER VARYING(100),
    ADD COLUMN IF NOT EXISTS lab_district                 CHARACTER VARYING(100),

    -- Step 4: Laboratory compliance details
    ADD COLUMN IF NOT EXISTS establishment_registration_number      CHARACTER VARYING(50),
    ADD COLUMN IF NOT EXISTS issuing_authority                      CHARACTER VARYING(25),
    ADD COLUMN IF NOT EXISTS issuing_date                           DATE,
    ADD COLUMN IF NOT EXISTS expiry_date                            DATE,
    ADD COLUMN IF NOT EXISTS establishment_registration_certificate CHARACTER VARYING,
    ADD COLUMN IF NOT EXISTS lab_pan                                CHARACTER VARYING(10),
    ADD COLUMN IF NOT EXISTS lab_pan_same_as_organization           BOOLEAN,
    ADD COLUMN IF NOT EXISTS lab_gstin                              CHARACTER VARYING(15),
    ADD COLUMN IF NOT EXISTS lab_gstin_same_as_organization         BOOLEAN,
    ADD COLUMN IF NOT EXISTS gstin_certificate                      CHARACTER VARYING,
    ADD COLUMN IF NOT EXISTS biomedical_waste_generated             BOOLEAN,
    ADD COLUMN IF NOT EXISTS biomedical_waste_authorization_number  CHARACTER VARYING(25),
    ADD COLUMN IF NOT EXISTS biomedical_waste_authorization_certificate CHARACTER VARYING,
    ADD COLUMN IF NOT EXISTS nabl_accreditation_certificate         CHARACTER VARYING,

    -- Step 5: Review & submit (authorization + T&C acceptance record)
    ADD COLUMN IF NOT EXISTS registration_authorization_accepted BOOLEAN,
    ADD COLUMN IF NOT EXISTS terms_accepted                      BOOLEAN,
    ADD COLUMN IF NOT EXISTS terms_version                       CHARACTER VARYING(20),
    ADD COLUMN IF NOT EXISTS terms_accepted_at                   TIMESTAMP WITHOUT TIME ZONE;
