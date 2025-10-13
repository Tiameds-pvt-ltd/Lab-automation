# Entity Schema Documentation

This document describes all database tables used by the application, their purposes, key fields, and relationships.

## labs (`Lab`)
- **Purpose**: Represents a laboratory/clinic organization, including contact, compliance, and membership info.
- **Primary Key**: `lab_id`
- **Fields and Types**:
  - `lab_id`: BIGINT (PK)
  - `name`: VARCHAR
  - `address`: VARCHAR
  - `city`: VARCHAR
  - `state`: VARCHAR
  - `description`: VARCHAR
  - `isActive`: BOOLEAN
  - `lab_logo`: VARCHAR (nullable)
  - `license_number`: VARCHAR
  - `lab_type`: VARCHAR
  - `lab_zip`: VARCHAR
  - `lab_country`: VARCHAR
  - `lab_phone`: VARCHAR
  - `lab_email`: VARCHAR
  - `director_name`: VARCHAR
  - `director_email`: VARCHAR
  - `director_phone`: VARCHAR
  - `certification_body`: VARCHAR
  - `lab_certificate`: VARCHAR
  - `director_govt_id`: VARCHAR
  - `lab_business_registration`: VARCHAR
  - `lab_license`: VARCHAR
  - `tax_id`: VARCHAR
  - `lab_accreditation`: VARCHAR
  - `data_privacy_agreement`: BOOLEAN
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
  - `created_by`: BIGINT (FK → `users.user_id`)
- **Relationships**:
  - Many-to-many with `users` via `lab_members` (members of the lab)
  - Many-to-many with `tests` via `lab_tests`
  - Many-to-many with `health_packages` via `lab_packages`
  - Many-to-many with `test_reference` via `lab_test_references`
  - Many-to-many with `doctors` via `lab_doctors`
  - Many-to-many with `insurance` via `lab_insurance`
  - Many-to-many with `patients` via `lab_patients`

## users (`User`)
- **Purpose**: Application users (staff, admins, etc.).
- **Primary Key**: `user_id`
- **Fields and Types**:
  - `user_id`: BIGINT (PK)
  - `username`: VARCHAR (unique)
  - `password`: VARCHAR
  - `is_verified`: BOOLEAN
  - `email`: VARCHAR (unique)
  - `firstName`: VARCHAR
  - `lastName`: VARCHAR
  - `phone`: VARCHAR
  - `address`: VARCHAR
  - `city`: VARCHAR
  - `state`: VARCHAR
  - `zip`: VARCHAR
  - `country`: VARCHAR
  - `enabled`: BOOLEAN
  - `token_version`: INTEGER (not null)
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
  - `created_by`: BIGINT (nullable, FK → `users.user_id`)
- **Relationships**:
  - Many-to-many with `roles` via `users_roles`
  - Many-to-many with `modules` via `users_modules`
  - Many-to-one `created_by` (self-reference to creator `user_id`)
  - Many-to-many with `labs` via `lab_members`

## roles (`Role`)
- **Purpose**: Authorization roles.
- **Primary Key**: `role_id`
- **Fields and Types**:
  - `role_id`: INTEGER (PK)
  - `name`: VARCHAR (unique)
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
- **Relationships**: Many-to-many with `users` via `users_roles`.

## modules (`ModuleEntity`)
- **Purpose**: Feature toggles/module access for users.
- **Primary Key**: `module_id`
- **Fields and Types**:
  - `module_id`: BIGINT (PK)
  - `name`: VARCHAR (unique)
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
- **Relationships**: Many-to-many with `users` via `users_modules`.

## patients (`PatientEntity`)
- **Purpose**: Patient demographics and lab relationships.
- **Primary Key**: `patient_id`
- **Fields and Types**:
  - `patient_id`: BIGINT (PK)
  - `first_name`: VARCHAR
  - `last_name`: VARCHAR
  - `email`: VARCHAR
  - `phone`: VARCHAR
  - `address`: VARCHAR
  - `city`: VARCHAR
  - `state`: VARCHAR
  - `zip`: VARCHAR
  - `Blood_Group`: VARCHAR
  - `date_of_birth`: DATE
  - `age`: VARCHAR
  - `gender`: VARCHAR
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
  - `created_by`: VARCHAR
  - `updated_by`: VARCHAR
  - `guardian_id`: BIGINT (nullable, self FK → `patients.patient_id`)
  - `patient_code`: VARCHAR (unique)
- **Relationships**:
  - One-to-many with `patient_visits` (each visit belongs to a patient)
  - Self-reference: `guardian_id` (many-to-one to `patients.patient_id`)
  - Many-to-many with `labs` via `lab_patients`

## patient_visits (`VisitEntity`)
- **Purpose**: Patient encounters/visits.
- **Primary Key**: `visit_id`
- **Fields and Types**:
  - `visit_id`: BIGINT (PK)
  - `visit_date`: DATE (not null)
  - `visit_type`: VARCHAR (not null)
  - `visit_status`: VARCHAR (not null)
  - `visit_description`: VARCHAR
  - `patient_id`: BIGINT (FK → `patients.patient_id`)
  - `billing_id`: BIGINT (unique nullable FK → `billing.billing_id`)
  - `doctor_id`: BIGINT (nullable FK → `doctors.doctor_id`)
  - `visit_cancellation_reason`: VARCHAR
  - `visit_cancellation_date`: DATE
  - `visit_cancellation_by`: VARCHAR
  - `visit_cancellation_time`: TIMESTAMP
  - `visit_time`: TIMESTAMP
  - `created_by`: VARCHAR
  - `updated_by`: VARCHAR
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
- **Relationships**:
  - Many-to-one to `patients` (`patient_id`)
  - Many-to-many with `tests` via `patient_visit_tests`
  - Many-to-many with `health_packages` via `patient_visit_packages`
  - Many-to-many with `insurance` via `visit_insurance`
  - Many-to-many with `samples` via `patient_visit_sample`
  - Many-to-many with `labs` via `lab_visit`
  - One-to-one with `billing` (`billing_id` on `patient_visits`)
  - One-to-many with `visit_test_result`
  - Many-to-one with `doctors` (`doctor_id`)

## tests (`Test`)
- **Purpose**: Master catalog of individual diagnostic tests.
- **Primary Key**: `test_id`
- **Fields and Types**:
  - `test_id`: BIGINT (PK)
  - `category`: VARCHAR
  - `name`: VARCHAR
  - `price`: DECIMAL
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
- **Relationships**:
  - Many-to-many with `labs` via `lab_tests`
  - Many-to-many with `health_packages` via `package_tests`
  - Many-to-many with `patient_visits` via `patient_visit_tests`

## health_packages (`HealthPackage`)
- **Purpose**: Bundled sets of tests with pricing/discounts.
- **Primary Key**: `package_id`
- **Fields and Types**:
  - `package_id`: BIGINT (PK)
  - `packageName`: VARCHAR
  - `price`: DOUBLE
  - `discount`: DOUBLE
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
- **Relationships**:
  - Many-to-many with `labs` via `lab_packages`
  - Many-to-many with `tests` via `package_tests`
  - Many-to-many with `patient_visits` via `patient_visit_packages`

## test_reference (`TestReferenceEntity`)
- **Purpose**: Lab-specific reference ranges and metadata for tests.
- **Primary Key**: `test_reference_id`
- **Fields and Types**:
  - `test_reference_id`: BIGINT (PK)
  - `category`: VARCHAR (not null)
  - `testName`: VARCHAR (not null)
  - `testDescription`: VARCHAR (not null)
  - `units`: VARCHAR
  - `gender`: VARCHAR (enum-converted)
  - `minReferenceRange`: DOUBLE (nullable)
  - `maxReferenceRange`: DOUBLE (nullable)
  - `ageMin`: INTEGER (not null)
  - `minAgeUnit`: VARCHAR (enum, nullable)
  - `ageMax`: INTEGER (not null)
  - `maxAgeUnit`: VARCHAR (enum, nullable)
  - `createdBy`: VARCHAR (not null)
  - `updatedBy`: VARCHAR (not null)
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
- **JSON Fields**:
  - `reportJson` (jsonb): test report schema or structured report data
  - `referenceRanges` (jsonb): array of age/gender-based ranges
- **Relationships**:
  - Many-to-many with `labs` via `lab_test_references`

## super_admin_test_referance (`SuperAdminReferanceEntity`)
- **Purpose**: Global reference data for tests (admin-managed), including JSON fields.
- **Primary Key**: `test_reference_id`
- **Fields and Types**:
  - `test_reference_id`: BIGINT (PK)
  - `category`: VARCHAR (not null)
  - `testName`: VARCHAR (not null)
  - `testDescription`: VARCHAR (nullable)
  - `units`: VARCHAR (nullable)
  - `gender`: VARCHAR (enum-converted)
  - `minReferenceRange`: DOUBLE (nullable)
  - `maxReferenceRange`: DOUBLE (nullable)
  - `ageMin`: INTEGER (nullable)
  - `minAgeUnit`: VARCHAR (enum, nullable)
  - `ageMax`: INTEGER (nullable)
  - `maxAgeUnit`: VARCHAR (enum, nullable)
  - `remarks`: VARCHAR (nullable)
  - `createdBy`: VARCHAR (nullable)
  - `updatedBy`: VARCHAR (nullable)
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
- **JSON Fields**: `reportJson` (jsonb), `referenceRanges` (jsonb)

## lab_report (`ReportEntity`)
- **Purpose**: Persisted reports per visit/test, including narrative and JSON details.
- **Primary Key**: `report_id`
- **Fields and Types**:
  - `report_id`: BIGINT (PK)
  - `visit_id`: BIGINT (not null, FK → `patient_visits.visit_id`)
  - `test_name`: VARCHAR (not null)
  - `test_category`: VARCHAR (not null)
  - `patient_name`: VARCHAR
  - `lab_id`: BIGINT (not null, FK → `labs.lab_id`)
  - `reference_description`: VARCHAR
  - `reference_range`: VARCHAR
  - `reference_age_range`: VARCHAR
  - `entered_value`: VARCHAR
  - `unit`: VARCHAR
  - `description`: VARCHAR(500)
  - `remarks`: VARCHAR(300)
  - `comment`: VARCHAR(500)
  - `created_by`: BIGINT
  - `updated_by`: BIGINT
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
- **JSON Fields**: `reportJson` (jsonb), `referenceRanges` (jsonb)
- **Relationships**: Implicit foreign keys by IDs (`visit_id`, `lab_id`) used by services; no explicit JPA relations on this entity.

## billing (`BillingEntity`)
- **Purpose**: Billing information summarizing charges per visit.
- **Primary Key**: `billing_id`
- **Fields and Types**:
  - `billing_id`: BIGINT (PK)
  - `total_amount`: DECIMAL (not null)
  - `payment_status`: VARCHAR (not null)
  - `payment_method`: VARCHAR (not null)
  - `payment_date`: VARCHAR (not null)
  - `discount`: DECIMAL (not null)
  - `gst_rate`: DECIMAL (not null)
  - `gst_amount`: DECIMAL (not null)
  - `cgst_amount`: DECIMAL (not null)
  - `sgst_amount`: DECIMAL (not null)
  - `igst_amount`: DECIMAL (not null)
  - `net_amount`: DECIMAL (not null)
  - `discount_reason`: VARCHAR
  - `received_amount`: DECIMAL
  - `actual_received_amount`: DECIMAL
  - `due_amount`: DECIMAL
  - `billing_time`: TIME (not null)
  - `billing_date`: VARCHAR
  - `created_by`: VARCHAR
  - `updated_by`: VARCHAR
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
- **Relationships**:
  - Many-to-many with `labs` via `lab_billing`
  - One-to-many with `test_discount` (as `testDiscounts`)
  - One-to-many with `billing_transaction`
  - One-to-one inverse from `patient_visits` (`billing`)

## test_discount (`TestDiscountEntity`)
- **Purpose**: Per-test discounts attached to a billing.
- **Primary Key**: `discount_id`
- **Fields and Types**:
  - `discount_id`: BIGINT (PK)
  - `test_id`: BIGINT (not null, FK → `tests.test_id`)
  - `discount_amount`: DECIMAL (not null)
  - `discount_percent`: DECIMAL (not null)
  - `final_price`: DECIMAL (not null)
  - `created_by`: VARCHAR (not null)
  - `updated_by`: VARCHAR (not null)
  - `billing_id`: BIGINT (nullable FK → `billing.billing_id`)
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
- **Relationships**: Many-to-one to `billing`.

## billing_transaction (`TransactionEntity`)
- **Purpose**: Payment breakdown for a billing (cash/card/UPI etc.).
- **Primary Key**: `transaction_id`
- **Fields and Types**:
  - `transaction_id`: BIGINT (PK)
  - `billing_id`: BIGINT (FK → `billing.billing_id`)
  - `payment_method`: VARCHAR (not null)
  - `upi_id`: VARCHAR
  - `upi_amount`: DECIMAL
  - `card_amount`: DECIMAL
  - `cash_amount`: DECIMAL
  - `received_amount`: DECIMAL
  - `refund_amount`: DECIMAL
  - `due_amount`: DECIMAL
  - `payment_date`: VARCHAR (not null)
  - `remarks`: VARCHAR
  - `created_by`: VARCHAR
  - `created_at`: TIMESTAMP
- **Relationships**: Many-to-one to `billing`.

## doctors (`Doctors`)
- **Purpose**: Physician directory and their lab/visit associations.
- **Primary Key**: `doctor_id`
- **Fields and Types**:
  - `doctor_id`: BIGINT (PK)
  - `name`: VARCHAR (not blank)
  - `email`: VARCHAR (not blank)
  - `speciality`: VARCHAR (not blank)
  - `qualification`: VARCHAR (not blank)
  - `hospitalAffiliation`: VARCHAR (not blank)
  - `licenseNumber`: VARCHAR (not blank)
  - `phone`: VARCHAR (not blank)
  - `address`: VARCHAR (not blank)
  - `city`: VARCHAR (not blank)
  - `state`: VARCHAR (not blank)
  - `country`: VARCHAR (not blank)
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
  - `created_by`: VARCHAR
  - `updated_by`: VARCHAR
- **Relationships**:
  - Many-to-many with `labs` via `lab_doctors`
  - One-to-many with `patient_visits`

## insurance (`InsuranceEntity`)
- **Purpose**: Insurance plans available to labs and visits.
- **Primary Key**: `insurance_id`
- **Fields and Types**:
  - `insurance_id`: BIGINT (PK)
  - `name`: VARCHAR (not null)
  - `description`: VARCHAR (not null)
  - `price`: DOUBLE (not null)
  - `duration`: INTEGER (not null)
  - `coverage_limit`: DOUBLE
  - `coverage_type`: VARCHAR
  - `status`: VARCHAR
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
- **Relationships**:
  - Many-to-many with `labs` via `lab_insurance`
  - Many-to-many with `patient_visits` via `visit_insurance`

## sample_entity (`SampleEntity`)
- **Purpose**: Sample types collected during visits.
- **Primary Key**: `sample_id`
- **Fields and Types**:
  - `sample_id`: BIGINT (PK)
  - `name`: VARCHAR (not null)
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
- **Relationships**: Many-to-many with `patient_visits` via `patient_visit_sample`.

## super_admin_test_pricelistentity (`SuperAdminTestEntity`)
- **Purpose**: Global test catalog with pricing for super admin.
- **Primary Key**: `test_id`
- **Fields and Types**:
  - `test_id`: BIGINT (PK)
  - `category`: VARCHAR (not null)
  - `name`: VARCHAR (not null)
  - `price`: DECIMAL (not null)
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
  - `createdBy`: VARCHAR (not null)
  - `updatedBy`: VARCHAR (not null)

## visit_test_result (`VisitTestResult`)
- **Purpose**: Tracks completion of each test for a visit.
- **Primary Key**: `id`
- **Fields and Types**:
  - `id`: BIGINT (PK)
  - `visit_id`: BIGINT (FK → `patient_visits.visit_id`)
  - `test_id`: BIGINT (FK → `tests.test_id`)
  - `is_filled`: BOOLEAN (not null, default false)
  - `reportStatus`: VARCHAR
  - `created_by`: VARCHAR
  - `updated_by`: VARCHAR
  - `test_status`: VARCHAR
  - `created_at`: TIMESTAMP
  - `updated_at`: TIMESTAMP
- **Relationships**: Many-to-one to `patient_visits`; many-to-one to `tests`.

---

## Join Tables (implicit via JPA mappings)
- `lab_members` (`lab_id`, `user_id`)
- `users_roles` (`user_id`, `role_id`)
- `users_modules` (`user_id`, `module_id`)
- `lab_patients` (`lab_id`, `patient_id`)
- `lab_tests` (`lab_id`, `test_id`)
- `package_tests` (`package_id`, `test_id`)
- `patient_visit_tests` (`visit_id`, `test_id`)
- `patient_visit_packages` (`visit_id`, `package_id`)
- `visit_insurance` (`visit_id`, `insurance_id`)
- `patient_visit_sample` (`visit_id`, `sample_id`)
- `lab_visit` (`visit_id`, `lab_id`)
- `lab_billing` (`billing_id`, `lab_id`)
- `lab_doctors` (`lab_id`, `doctor_id`)
- `lab_test_references` (`lab_id`, `test_reference_id`)

## Notes
- Several entities store foreign keys as primitive IDs without explicit JPA relations (e.g., `ReportEntity`), but services enforce associations.
- JSONB columns use PostgreSQL `jsonb` with `@JdbcTypeCode(SqlTypes.JSON)`.
