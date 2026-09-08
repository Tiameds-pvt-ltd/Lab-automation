# TiaMeds Lab Automation Platform — High-Level Design (HLD)

> **Document type:** High-Level Design (Current State — reverse-engineered from actual codebase)
> **Application:** `tiameds` · `com.tiameds` · Spring Boot 3.3.4 / Java 17
> **Last updated:** September 2026
> **Environment:** AWS ECS Fargate · ap-south-1 · PostgreSQL

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Architectural Style](#2-architectural-style)
3. [System Context Diagram](#3-system-context-diagram)
4. [Component Architecture](#4-component-architecture)
5. [Domain Model — Entity Relationship Overview](#5-domain-model--entity-relationship-overview)
6. [API Surface — Controller Groups](#6-api-surface--controller-groups)
7. [Security Architecture](#7-security-architecture)
8. [Data Architecture](#8-data-architecture)
9. [Dashboard Rollup Subsystem](#9-dashboard-rollup-subsystem)
10. [Audit Subsystem](#10-audit-subsystem)
11. [File Storage Architecture](#11-file-storage-architecture)
12. [Email & Notification Architecture](#12-email--notification-architecture)
13. [AWS Infrastructure Architecture](#13-aws-infrastructure-architecture)
14. [CI/CD Pipeline](#14-cicd-pipeline)
15. [Configuration & Secrets Management](#15-configuration--secrets-management)
16. [Key Business Flows](#16-key-business-flows)
17. [Non-Functional Characteristics](#17-non-functional-characteristics)
18. [Technology Stack Summary](#18-technology-stack-summary)
19. [Known Gaps & Recommended Improvements](#19-known-gaps--recommended-improvements)

---

## 1. System Overview

TiaMeds Lab Automation Platform is a **multi-tenant, cloud-native, REST-first laboratory management system** built as a single deployable Spring Boot monolith. It digitizes the end-to-end workflow of a diagnostic laboratory — from patient registration through sample collection, test result entry, and report generation — while maintaining a full billing trail and a centralized super-admin dashboard across all enrolled labs.

### Core Capabilities

| Capability | Description |
|---|---|
| Multi-tenant lab management | A single deployment serves N independent labs; each lab's data is isolated by `lab_id` scoping |
| Patient lifecycle | Registration, visit creation, test ordering, sample association, result entry, report generation |
| Billing | GST-aware billing (CGST/SGST/IGST), multi-method payments (CASH / UPI / CARD), due tracking |
| Dashboard analytics | Daily rollup statistics per lab and per test category, pre-computed at write time |
| Lab onboarding | Self-service lab registration with email verification and rate-limited email delivery |
| Security | RS256 JWT in HTTP-only cookies, RBAC with four roles, OTP auth, password reset, audit trail |
| File assets | Lab logos and report signatures stored on S3 with CDN delivery |
| AI observations | Optional AI-generated clinical observations per patient visit |

---

## 2. Architectural Style

The system is a **well-structured modular monolith** — a single deployable unit (`app.jar`) — not a microservices system.

```
┌─────────────────────────────────────────────────────────┐
│               TiaMeds Monolith (app.jar)                │
│                                                         │
│   ┌──────────┐  ┌──────────┐  ┌──────────────────────┐ │
│   │Controllers│→│ Services │→│  Repositories (JPA)  │ │
│   │ (REST)   │  │(Business)│  └──────────────────────┘ │
│   └──────────┘  └──────────┘            ↓              │
│                               ┌──────────────────────┐  │
│                               │  PostgreSQL (AWS RDS) │  │
│                               └──────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

Packages follow a **domain-first** layout:

```
tiameds.com.tiameds
├── audit/            AOP-based audit trail (@Auditable + AuditAspect)
├── config/           Spring configuration beans (Security, CORS, JWT, Async)
├── controller/
│   ├── admin/        Admin-scoped APIs (stats, audit logs, category rollup admin)
│   ├── auth/         Login, register, OTP, user management, module management
│   ├── lab/          Per-lab operational APIs (visit, patient, report, billing, etc.)
│   ├── labforall/    Cross-lab public read APIs
│   ├── onboarding/   New lab onboarding, email verification
│   ├── sampleassociation/  Sample → visit linking and report generation
│   └── superAdmin/   Super-admin: lab CRUD, master data, global dashboard
├── dto/              Request/response DTOs (auth, lab, onboarding, visits)
├── entity/           JPA entities (25+ domain tables)
├── filter/           Servlet filters (IP whitelist, rate limit stub, JWT)
├── repository/       Spring Data JPA repositories
├── services/
│   ├── auth/         JWT, OTP, password reset, refresh token, UserDetails
│   ├── email/        SMTP email dispatch
│   ├── lab/          All lab domain services + rollup services
│   ├── onboarding/   Onboarding orchestration, verification tokens
│   └── superAdmin/   Super-admin lab administration service
└── utils/            JwtUtil, SnowflakeIdGenerator, EncryptionUtil, PasswordValidator
```

---

## 3. System Context Diagram

```
                           ┌─────────────────────────────────────────────────┐
                           │                 EXTERNAL ACTORS                 │
                           └─────────────────────────────────────────────────┘

  ┌──────────────┐  HTTPS   ┌──────────────────────────────────────────────┐
  │ Lab Frontend │─────────►│                                              │
  │ (React SPA)  │          │       TiaMeds Backend  (Spring Boot)         │
  │lab-prod.     │◄─────────│       REST API  base: /api/v1/**             │
  │ tiameds.ai   │ JWT Cookie│                                              │
  └──────────────┘          │  ┌──────────────────────────────────────────┐│
                            │  │  Security Filter Chain                   ││
  ┌──────────────┐  HTTPS   │  │  IpWhitelist → RateLimit → JWT           ││
  │ Super Admin  │─────────►│  └──────────────────────────────────────────┘│
  │ Dashboard    │          │                                              │
  └──────────────┘          │  ┌───────────────┐    ┌───────────────────┐  │
                            │  │  Controllers  │    │     Services      │  │
  ┌──────────────┐          │  └───────────────┘    └───────────────────┘  │
  │ Lab Staff    │          │                               ↓              │
  │ (Technician, │          │  ┌───────────────────────────────────────┐   │
  │  Desk, Admin)│          │  │   Spring Data JPA / Hibernate 6       │   │
  └──────────────┘          │  └───────────────────────────────────────┘   │
                            └──────────────────┬───────────────────────────┘
                                               │
         ┌─────────────────────────────────────┼────────────────────────────────────┐
         │                                     │                                    │
         ▼                                     ▼                                    ▼
┌─────────────────────┐          ┌──────────────────────┐       ┌───────────────────────┐
│  Amazon RDS          │          │    Amazon S3          │       │  SMTP  (Gmail)        │
│  PostgreSQL          │          │  Lab logos,           │       │  Email verification,  │
│  (ap-south-1)        │          │  Report signatures    │       │  Password reset, OTP  │
└─────────────────────┘          └──────────────────────┘       └───────────────────────┘

         ┌──────────────────────────────────────────────────────────────────────────┐
         │                  AWS SERVICES                                            │
         │  GitHub Actions → ECR → ECS Fargate → Secrets Manager → CloudWatch Logs │
         └──────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Component Architecture

### 4.1 Request Filter Chain

Every HTTP request passes through three servlet filters before reaching any controller:

```
HTTP Request
    │
    ▼
┌──────────────────────────────────────────────────────────────────────┐
│  IpWhitelistFilter  (OncePerRequestFilter)                           │
│  Configurable IP allow-list. Disabled by default.                    │
│  Enable with: security.ip.whitelist.enabled=true                     │
└──────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────────────────────────────┐
│  RateLimitFilter  (OncePerRequestFilter)                             │
│  Currently a pass-through stub. User-based rate limiting (Bucket4j)  │
│  is enforced inside AuthController and UserController where the      │
│  username is available from the request body.                        │
└──────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────────────────────────────────────────────────┐
│  JwtFilter  (OncePerRequestFilter)                                   │
│  1. Resolve token: HttpOnly cookie (accessToken) first,              │
│     Authorization: Bearer header as fallback                         │
│  2. Parse RS256 JWT; decrypt AES-encrypted subject field             │
│  3. Load User from UserRepository (single DB call; avoids double     │
│     fetch compared to calling UserDetailsService separately)         │
│  4. Validate tokenVersion claim against user.token_version           │
│  5. Populate SecurityContextHolder with MyUserDetails                │
└──────────────────────────────────────────────────────────────────────┘
    │
    ▼
Spring Security authorization (role-based URL matching)
    │
    ▼
Controller → Service → Repository → PostgreSQL
```

### 4.2 Controller Groups

| Controller Group | URL Prefix | Minimum Role | Purpose |
|---|---|---|---|
| `AuthController` | `/auth` | Public | Login, register, OTP, refresh, password reset, logout |
| `PatientController` | `/lab/{labId}/` | DESKROLE | Patient CRUD |
| `VisitController` | `/lab/{labId}/visits` | DESKROLE | Visit lifecycle |
| `BillingController` | `/lab/{labId}/` | DESKROLE | Billing and transactions |
| `TestController` | `/admin/lab/{labId}/tests` | ADMIN | Lab test catalogue management |
| `TestReferenceController` | `/lab/test-reference/{labId}` | TECHNICIAN | Reference ranges |
| `SampleAssociation` | `/lab/` | TECHNICIAN | Sample-to-visit linking |
| `ReportGeneration` | `/lab/{labId}/report` | TECHNICIAN | Report creation and retrieval |
| `DoctorController` | `/admin/lab/{labId}/doctors` | DESKROLE | Doctor management |
| `InsuranceController` | `/lab/admin/insurance` | DESKROLE | Insurance providers |
| `HealthPackageController` | `/admin/lab/{labId}/package` | ADMIN | Test package bundles |
| `LabAdminController` | `/lab/admin/` | ADMIN | Lab info, user listing |
| `LabMemberController` | `/user-management/` | ADMIN | User/member CRUD |
| `StaticController` | `/lab/static/{labId}` | ADMIN | Static lab lookup data |
| `AdminStatsController` | `/lab-admin/stats/` | ADMIN | Lab-level daily statistics |
| `CategoryStatsRollupAdminController` | `/lab-admin/stats/` | ADMIN | Category rollup admin |
| `AuditLogsController` | `/admin/audit-logs` | ADMIN | View audit events |
| `LabSuperAdminController` | `/lab-super-admin/` | SUPERADMIN | Global lab management |
| `SuperAdminDashboardController` | `/lab-super-admin/` | SUPERADMIN | Cross-lab live dashboard |
| `DashboardRollupAdminController` | `/lab-super-admin/stats/rollup/` | SUPERADMIN | Manual backfill trigger |
| `SuperAdminStatsController` | `/lab-super-admin/stats/` | SUPERADMIN | Cross-lab statistics |
| `ReferenceAndTestController` | `/lab-super-admin/` | SUPERADMIN | Global test and reference master data |
| `OnboardingController` | `/onboarding/` | Public | Lab onboarding form |
| `EmailVerificationController` | `/onboarding/verify` | Public | Email token verification |
| `ReportSettingController` | `/lab/{labId}/report-settings` | ADMIN | Report appearance configuration |
| `AiClinicalObservationController` | `/lab/` | ADMIN | AI clinical observations per visit |
| `PatientHealthSnapshotController` | `/lab/` | ADMIN | Patient health summary view |
| `LabForAll` | `/public/labs` | Public | Public lab directory |
| `ModuleController` | `/auth/modules` | ADMIN | Feature module management |
| `SecurityController` | `/auth/` | Authenticated | Security and session management |

### 4.3 Service Layer

```
AUTH SERVICES
┌─────────────────────┐  ┌─────────────────────┐  ┌───────────────────────────┐
│ UserService         │  │ OtpService           │  │ PasswordResetService      │
│ RefreshTokenService │  │ MemberUserServices   │  │ PasswordResetRateLimitSvc │
└─────────────────────┘  └─────────────────────┘  └───────────────────────────┘

LAB DOMAIN SERVICES
┌──────────────────┐ ┌──────────────────┐ ┌──────────────┐ ┌────────────────┐
│ LabService        │ │ PatientService   │ │ VisitService │ │ BillingService │
│ LabCreationSvc    │ │ UpdatePatientSvc │ │              │ │ BillingMgmtSvc │
└──────────────────┘ └──────────────────┘ └──────────────┘ └────────────────┘
┌──────────────────┐ ┌──────────────────┐ ┌──────────────┐ ┌────────────────┐
│ TestServices      │ │ TestRefSvc       │ │ ReportService│ │ DoctorService  │
│ SampleAssocSvc    │ │                  │ │ ReportSettSvc│ │ InsurancesSvc  │
└──────────────────┘ └──────────────────┘ └──────────────┘ └────────────────┘

DASHBOARD ROLLUP SERVICES  (write-time pre-aggregation)
┌────────────────────────────┐  ┌──────────────────────────────┐
│ DashboardRollupService     │  │ CategoryStatsRollupService   │
│ DashboardRollupBackfillSvc │  │ CategoryStatsBackfillService │
│ DashboardVerificationSvc   │  │ RollupStartupBackfillRunner  │
│ RollupRecomputeListener    │  │ CategoryStatsRollupListener  │
└────────────────────────────┘  └──────────────────────────────┘

CROSS-CUTTING SERVICES
┌──────────────────┐  ┌──────────────────┐  ┌────────────────────────────┐
│ S3StorageService  │  │ EmailService     │  │ SequenceGeneratorService   │
│ AuditLogService   │  │ OnboardingService│  │ LabDefaultDataService      │
└──────────────────┘  └──────────────────┘  └────────────────────────────┘
```

---

## 5. Domain Model — Entity Relationship Overview

### 5.1 Core Entity Summary

| Entity | Table | Key Fields | Notes |
|---|---|---|---|
| `User` | `users` | id, username, email, tokenVersion, userCode | `tokenVersion` enables all-session JWT revocation |
| `Role` | `roles` | id, name | SUPERADMIN, ADMIN, TECHNICIAN, DESKROLE |
| `ModuleEntity` | `modules` | id, name | Feature permission flags per user |
| `Lab` | `labs` | lab_id, name, licenseNumber, isActive, labLogo | Central multi-tenant anchor |
| `PatientEntity` | `patients` | patient_id, patientCode, guardianId | Self-referential guardian; links to multiple labs |
| `VisitEntity` | `patient_visits` | visit_id, visitCode, visitType, visitStatus | IN-PATIENT / OUT-PATIENT |
| `Test` | `tests` | test_id, testCode, category, name, price | Shared across labs via `lab_tests` join table |
| `TestReferenceEntity` | `test_reference` | id (Snowflake), gender, ageMin/Max, JSONB fields | Snowflake ID; JSONB for report templates |
| `SampleEntity` | `sample_entity` | sample_id, sampleCode, labId | Per-lab sample types |
| `VisitSample` | `visit_samples` | visit_id, sample_id | Explicit join entity for visit-sample relationship |
| `VisitTestResult` | `visit_test_result` | id, visitTestResultCode, isFilled, reportStatus | PENDING / COMPLETED / CANCELLED |
| `BillingEntity` | `billing` | billing_id, billingCode, totalAmount, gst*, netAmount, dueAmount | One-to-one with VisitEntity |
| `TransactionEntity` | `billing_transaction` | transaction_id, transactionCode, paymentMethod | CASH / UPI / CARD; N per billing |
| `ReportEntity` | `lab_report` | reportId, reportCode, JSONB testRows / referenceRanges / reportJson | Rich JSON report payload |
| `Doctors` | `doctors` | doctor_id | Per-lab doctors linked via `lab_doctors` |
| `HealthPackage` | `health_packages` | package_id | Grouped test bundles |
| `InsuranceEntity` | `insurance` | insurance_id | Insurance providers |
| `LabAuditLogs` | `lab_audit_logs` | id (UUID), JSONB oldValue / newValue / fieldChanged | Async AOP-written audit trail |
| `DailyLabStats` | `daily_lab_stats` | (lab_id, stat_date) composite PK | Pre-aggregated KPIs per lab per day |
| `DailyLabCategoryStats` | `daily_lab_category_stats` | (lab_id, stat_date, category) composite PK | Category-level revenue/count rollup |
| `Otp` | `otp` | — | OTP codes for authentication |
| `RefreshToken` | `refresh_tokens` | id (UUID) | Stored refresh JWT records |
| `PasswordResetToken` | `password_reset_tokens` | — | Password reset link tokens |
| `PasswordResetRateLimit` | `password_reset_rate_limits` | — | DB-backed rate limiting (cross-instance safe) |
| `VerificationToken` | `verification_tokens` | — | Email verification tokens for onboarding |
| `LabEntitySequence` | `lab_entity_sequence` | (lab_id, entity_type) composite PK | Per-lab sequential human-readable code counters |
| `ReportSettings` | `report_settings` | — | Per-lab report appearance configuration |
| `ReportRoleSetting` | `report_role_settings` | — | Per-role report visibility rules |
| `SuperAdminTestEntity` | `super_admin_test` | — | Global test master data managed by SUPERADMIN |
| `SuperAdminReferenceEntity` | `super_admin_reference` | — | Global reference master data |
| `AiClinicalObservation` | `ai_clinical_observations` | — | AI-generated clinical notes per visit |

### 5.2 Join Tables

| Join Table | Entities Connected |
|---|---|
| `lab_members` | Lab ↔ User |
| `lab_tests` | Lab ↔ Test |
| `lab_packages` | Lab ↔ HealthPackage |
| `lab_test_references` | Lab ↔ TestReferenceEntity |
| `lab_doctors` | Lab ↔ Doctors |
| `lab_insurance` | Lab ↔ InsuranceEntity |
| `lab_patients` | PatientEntity ↔ Lab |
| `lab_visit` | VisitEntity ↔ Lab |
| `lab_billing` | BillingEntity ↔ Lab |
| `patient_visit_tests` | VisitEntity ↔ Test |
| `patient_visit_packages` | VisitEntity ↔ HealthPackage |
| `visit_insurance` | VisitEntity ↔ InsuranceEntity |
| `users_roles` | User ↔ Role |
| `users_modules` | User ↔ ModuleEntity |

### 5.3 Relationship Graph (Simplified)

```
            ┌──────────┐
            │   Role   │ ←──M:M (users_roles)──┐
            └──────────┘                        │
                                          ┌─────┴────┐   M:M (users_modules)   ┌────────────┐
                                          │   User   │────────────────────────►│  Module    │
                                          └─────┬────┘                         └────────────┘
                          M:M (lab_members)     │ created_by
                    ┌─────────────────────────┐ │
                    ▼                         └─┤
              ┌───────────┐                     │
              │    Lab    │◄────────────────────┘
              └───┬───────┘
      (M:M associations via 8 join tables)
      Tests │ Packages │ Doctors │ Insurance │ TestRefs │ Patients │ Visits │ Billing
            │                        │
            │                  ┌─────┴─────┐
            │                  │  Patient  │◄──guardian (self-ref)
            │                  └─────┬─────┘
            │                    1:N │
            │                  ┌─────┴─────┐
            │                  │   Visit   │──1:1──►Billing──1:N──►Transaction
            │                  └─┬─────────┘
            │         ┌──────────┤
            │    VisitSample   VisitTestResult──►Test
            │    (join entity)
            └─────────────────────────────────────────►
```

---

## 6. API Surface — Controller Groups

**Base context path:** `/api/v1`

### Public Endpoints (no authentication required)

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/login` | Username/password login; sets JWT access and refresh cookies |
| POST | `/auth/register` | User self-registration |
| POST | `/auth/send-otp` | Send 6-digit OTP for OTP-based login |
| POST | `/auth/verify-otp` | Verify OTP and issue JWT cookies |
| POST | `/auth/refresh` | Rotate access token using refresh cookie |
| POST | `/auth/forgot-password` | Request password reset email |
| POST | `/auth/reset-password` | Submit new password using reset token |
| GET | `/auth/validate-reset-token` | Validate that a reset token is still active |
| GET/POST | `/onboarding/**` | Lab onboarding registration and email verification flow |
| GET | `/public/labs` | Public lab directory |

### Protected Endpoints (role-grouped summary)

**DESKROLE + ADMIN + SUPERADMIN**
- `POST /lab/{labId}/add-patient` — Register patient in a lab
- `GET /lab/{labId}/patients` — List patients for a lab
- `PUT /lab/{labId}/update-patient-details/{patientId}` — Update patient record
- `POST /lab/{labId}/visits` — Create a new visit
- `GET /lab/admin/insurance/{labId}` — List insurance providers

**TECHNICIAN + ADMIN + SUPERADMIN**
- `POST /lab/add-samples`, `PUT /lab/update-samples` — Sample management
- `GET /lab/{labId}/get-visit-samples` — Samples linked to a visit
- `GET /lab/test-reference/{labId}` — View reference ranges
- `POST lab/test-reference/{labId}/add` — Add test reference
- `GET /lab/{labId}/visitsdatewise` — Date-wise visit list
- `GET /lab/{labId}/report/{visitId}` — Retrieve generated report

**ADMIN + SUPERADMIN**
- `/admin/lab/{labId}/tests/**` — Full test catalogue management
- `/admin/lab/{labId}/package/**` — Health package management
- `/admin/lab/{labId}/doctors/**` — Doctor management
- `/admin/lab/{labId}/test-reference/**` — Test reference admin
- `/lab/{labId}/report-settings/**` — Report appearance settings
- `/lab-admin/stats/**` — Lab-level daily statistics dashboard
- `/user-management/**` — Member CRUD

**SUPERADMIN only**
- `/lab-super-admin/**` — All-lab management, master data, cross-lab dashboard
- `POST /lab-super-admin/stats/rollup/backfill` — Trigger manual daily_lab_stats backfill
- `POST /lab-admin/stats/category-rollup/backfill` — Trigger manual category stats backfill

---

## 7. Security Architecture

### 7.1 Authentication Flow

```
Client                            Backend                              PostgreSQL
  │                                  │                                     │
  │── POST /auth/login ─────────────►│                                     │
  │   {username, password}            │── SELECT FROM users WHERE... ──────►│
  │                                  │◄── User record ─────────────────────│
  │                                  │                                     │
  │                                  │  1. BCrypt.verify(password, hash)    │
  │                                  │  2. Check tokenVersion               │
  │                                  │  3. Generate RS256 Access Token      │
  │                                  │     (15 min TTL; AES-encrypted       │
  │                                  │      subject; tokenVersion claim)    │
  │                                  │  4. Generate RS256 Refresh Token     │
  │                                  │     (24h TTL; stored in DB)          │
  │                                  │── INSERT refresh_tokens ────────────►│
  │◄── Set-Cookie: accessToken=...  ─│                                     │
  │    Set-Cookie: refreshToken=...  │                                     │
  │    HttpOnly; Secure;             │                                     │
  │    SameSite=None;                │                                     │
  │    Domain=.tiameds.ai            │                                     │
```

### 7.2 JWT Architecture Detail

| Property | Value |
|---|---|
| Algorithm | RS256 — private key signs on the server; public key verifies |
| Access token TTL | 15 minutes (configurable via `JWT_ACCESS_TOKEN_TTL`) |
| Refresh token TTL | 24 hours (configurable via `JWT_REFRESH_TOKEN_TTL`) |
| Subject field | AES-encrypted username (key from `JWT_SUBJECT_ENCRYPTION_KEY`) |
| Token revocation | Increment `user.token_version` to immediately invalidate all existing sessions |
| Cookie name — access | `accessToken` (configurable) |
| Cookie name — refresh | `refreshToken` (configurable) |
| Cookie domain | `.tiameds.ai` (covers all subdomains) |
| Cookie flags | `HttpOnly`, `Secure`, `SameSite=None` |
| Token delivery | Cookie-first; `Authorization: Bearer` header as fallback |
| Issuer / Audience | Configurable via `JWT_ISSUER` / `JWT_AUDIENCE` environment variables |
| Key storage | RSA key files loaded from classpath resource paths at startup |

### 7.3 Role-Based Access Control (RBAC)

```
SUPERADMIN
  Full platform access; lab registration; master data management;
  cross-lab dashboard; rollup backfill operations.

ADMIN
  Lab-scoped management; member administration; test and package
  configuration; billing; report generation; audit log access.

TECHNICIAN
  Sample management; test result entry; date-wise visit views;
  report generation; reference range viewing.

DESKROLE
  Patient registration; visit creation; billing collection;
  insurance management; front-desk operations.
```

### 7.4 Security Headers (Applied Globally)

| Header | Configuration |
|---|---|
| `Content-Security-Policy` | `default-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'self'; base-uri 'self'` |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains; preload` (1-year HSTS) |
| `X-Frame-Options` | `SAMEORIGIN` |

### 7.5 Rate Limiting Strategy

| Scenario | Mechanism | Configured Limit |
|---|---|---|
| Login attempts | Bucket4j in-memory token bucket per user | 5 attempts / 10-minute window |
| Password reset — by email | Database-backed (`password_reset_rate_limits` table) | 3 requests / 1-minute window |
| Password reset — by IP | Database-backed | 3 requests / 1-minute window |
| Onboarding verification email | DB-backed (`EmailRateLimitService`) | 3 emails / 60-minute window per address |

Database-backed rate limiting for password reset ensures correctness across multiple ECS replicas — an in-memory token bucket would not satisfy this.

---

## 8. Data Architecture

### 8.1 Database Configuration

| Property | Value |
|---|---|
| Engine | PostgreSQL |
| Connection pool | HikariCP, max 40 connections |
| Auto-commit | Disabled (explicit transaction management) |
| Transaction isolation | `READ_COMMITTED` |
| Prepared statement cache | Enabled (size 500, SQL limit 1024 chars) |
| Batch size | 15 inserts/updates |
| DDL management | `hibernate.ddl-auto=update` — Hibernate applies incremental schema diffs; Flyway manages named migrations |
| Migrations | Flyway (baseline V10; validates on migrate) |
| Timezone | UTC |
| SQL logging | Disabled in production |
| Batch fetch size | Default 10 (configurable per query) |

### 8.2 JSONB Usage

Several entities store flexible, schema-evolving content in PostgreSQL `jsonb` columns:

| Entity | Column | Content |
|---|---|---|
| `ReportEntity` | `reportJson` | Full rendered report payload |
| `ReportEntity` | `referenceRanges` | Dynamic reference range data |
| `ReportEntity` | `testRows` | Multi-row test result table (mapped as `List<TestRow>`) |
| `TestReferenceEntity` | `reportJson` | Report structure template |
| `TestReferenceEntity` | `referenceRanges` | Age/gender-specific reference ranges |
| `TestReferenceEntity` | `dropdown` | UI dropdown option definitions |
| `TestReferenceEntity` | `impression` | Clinical impression template text |
| `LabAuditLogs` | `oldValue` | Before-state snapshot of mutated entity |
| `LabAuditLogs` | `newValue` | After-state snapshot |
| `LabAuditLogs` | `fieldChanged` | Field-by-field diff (computed by `FieldChangeTracker`) |

### 8.3 ID Generation Strategy

| Strategy | Used By | Notes |
|---|---|---|
| `GenerationType.IDENTITY` (DB serial) | Most entities (User, Lab, Patient, Visit, Test, Billing…) | PostgreSQL auto-increment |
| `@GeneratedValue` (UUID) | `LabAuditLogs` | UUID PK guarantees global uniqueness for audit records |
| Custom Snowflake ID | `TestReferenceEntity` | `SnowflakeIdentifierGenerator` — distributed-safe, time-sortable 64-bit long ID |
| Per-lab sequential codes | All entities with `*Code` fields | `LabEntitySequence` table tracks per-lab, per-type counters yielding human-readable codes (`patientCode`, `visitCode`, `billingCode`, `sampleCode`, `testReferenceCode`) |

### 8.4 Flyway Migration Baseline

`baseline-version=10` — migrations V1–V10 were applied manually before Flyway was adopted. All subsequent schema changes are managed as Flyway versioned scripts. Notable migrations:

| Migration | Table Created | Purpose |
|---|---|---|
| V17 | `daily_lab_stats` | Dashboard stats rollup per lab per day |
| V41 | `daily_lab_category_stats` | Category-level revenue and count rollup |

---

## 9. Dashboard Rollup Subsystem

To avoid expensive real-time aggregate queries on the dashboard, the system maintains **pre-computed rollup tables** that are updated synchronously with every data write.

### 9.1 Write-Time Rollup Architecture

```
Business write (billing, visit status change, test result)
    │
    ├── Business Service completes DB write
    │
    ├── Publishes RollupRecomputeEvent (Spring ApplicationEvent)
    │       │
    │       ▼ (async)
    │   RollupRecomputeListener
    │       └── DashboardRollupService.recomputeDay(labId, date)
    │               Recomputes all KPIs from source tables for that lab+day
    │               └── dailyLabStatsRepository.upsertRow(...)
    │                       → daily_lab_stats (lab_id, stat_date)
    │
    └── Publishes CategoryRollupEvent (Spring ApplicationEvent)
            │
            ▼ (async)
        CategoryStatsRollupListener
            └── CategoryStatsRollupService.recomputeDay(labId, date, category)
                    └── dailyLabCategoryStatsRepository.upsertRow(...)
                            → daily_lab_category_stats (lab_id, stat_date, category)
```

### 9.2 DashboardRollupService — KPI Sources

`recomputeDay(labId, date)` is **fully idempotent** — it recomputes the entire day from source tables on every call; it never increments. This prevents double-counting on retries or repeated events.

| KPI Column | Source Repository Method |
|---|---|
| `paid_revenue` | `BillingRepository.sumPaidAmountByLabId()` |
| `due_revenue` | `BillingRepository.sumDueAmountByLabIdAndCreatedAtBetween()` |
| `test_count` | `VisitTestResultRepository.countAllTestsByLabIdAndCreatedAtBetween()` |
| `reports_generated` | `VisitTestResultRepository.countCompletedReportsByLabIdAndCreatedAtBetween()` |
| `pending_samples` | `VisitRepository.countPendingVisitsByLabIdAndCreatedAtBetween()` |
| `patient_count` | `VisitRepository.countDistinctPatientsByLabIdAndCreatedAtBetween()` |

Rollup failures are caught, logged with `logger.error(...)`, and silently swallowed — a rollup failure never propagates to the triggering business transaction.

### 9.3 Startup Backfill

`RollupStartupBackfillRunner` (implements `ApplicationRunner`) executes on every application start:

```
For each Lab in the database:
    startDate = lab.createdAt.toLocalDate()   (or today if null)
    DashboardRollupBackfillService.backfillLab(labId, startDate, today)
    CategoryStatsBackfillService.backfillLab(labId, startDate, today)
    Per-lab failures are caught and logged; they do not stop the backfill for other labs.
```

This ensures dashboard statistics are always correct after downtime, deployments, or fresh database initializations — with no manual intervention required.

### 9.4 Manual Backfill Endpoints (Admin-Triggered)

| Method | Endpoint | Role Required | Purpose |
|---|---|---|---|
| POST | `/lab-super-admin/stats/rollup/backfill` | SUPERADMIN | Trigger manual `daily_lab_stats` backfill |
| POST | `/lab-admin/stats/category-rollup/backfill` | ADMIN | Trigger manual category stats backfill |

---

## 10. Audit Subsystem

### 10.1 Overview

The audit system uses Spring AOP (`@Around` advice) to capture all data-mutating operations declaratively, without coupling audit logic to business code.

```
@Auditable(module = "PATIENT", action = "CREATE")   ← Annotation on controller method
        │
        ▼
AuditAspect (@Around advice)
    │
    ├── BEFORE proceed:
    │     Extract PatientDTO from method arguments
    │     For UPDATE: snapshot existing patient from DB as oldPatientSnapshot
    │
    ├── pjp.proceed()   ← actual business logic executes
    │
    └── AFTER proceed:
          Extract authenticated user and role from SecurityContextHolder
          Extract IP address (X-Forwarded-For or RemoteAddr)
          Extract User-Agent and X-Request-ID from HttpServletRequest
          Extract labId, patientId, visitId, billingId, testId from URI path variables
          
          For CREATE: serialize response/request patient data → newValue (JSON)
          For UPDATE: serialize before-state → oldValue; serialize after-state → newValue;
                      compute field-by-field diff → fieldChanged (JSON)
          
          AuditLogService.persistAsync(auditLog)   ← non-blocking async DB write
```

### 10.2 Audit Log Fields

| Field | Type | Description |
|---|---|---|
| `id` | UUID | Unique log entry identifier |
| `timestamp` | LocalDateTime | Event timestamp |
| `userId` / `username` | Long / String | Identity of the acting user |
| `role` | String | User's primary role at time of action |
| `lab_id` | String | Lab context for the operation |
| `ipAddress` | String | X-Forwarded-For or client remote address |
| `deviceInfo` | String | User-Agent header value |
| `requestId` | String | X-Request-ID header (correlation ID) |
| `module` | String | Affected domain module (e.g., PATIENT, BILLING) |
| `entityType` | String | Entity class name: Patient, Visit, Billing, Test |
| `entityId` | String | Specific record identifier |
| `actionType` | String | CREATE, UPDATE, DELETE, VIEW |
| `oldValue` | jsonb | Full before-state snapshot as JSON |
| `newValue` | jsonb | Full after-state snapshot as JSON |
| `fieldChanged` | jsonb | Field-by-field diff computed by `FieldChangeTracker` |
| `changeReason` | TEXT | User-provided reason for change |
| `severity` | Enum | LOW, MEDIUM, HIGH, CRITICAL |

---

## 11. File Storage Architecture

### 11.1 S3 Asset Types

| Asset | Uploaded Via | Stored In | Delivered Via |
|---|---|---|---|
| Lab Logo | `LabAdminController` → `S3StorageService` | S3 bucket, path tied to labId | CDN base URL (`AWS_S3_CDN_BASE_URL`) |
| Report Signature | `ReportSettingController` | S3 bucket | Pre-signed URL (default 10 min expiry) |

### 11.2 Upload and Read Flow

```
Upload (multipart/form-data)
  Client → Controller → S3StorageService.upload()
    ├── Put object to S3 bucket (AWS_S3_BUCKET, region AWS_REGION)
    └── Return: CDN URL stored in DB (labs.lab_logo or report_settings.signature_url)

Read (presigned URL)
  Controller → S3StorageService.generatePresignedUrl()
    └── Return time-limited pre-signed URL (expiry: AWS_S3_PRESIGN_EXPIRY_MINUTES, default 10)
```

---

## 12. Email & Notification Architecture

### 12.1 Email Use Cases

| Trigger | Email Content |
|---|---|
| New lab registration | Email verification link pointing to `onboarding.frontend.verification-url` |
| Onboarding completion | Welcome message with onboarding form URL |
| Password reset request | Reset link with time-limited token pointing to `password.reset.url` |
| OTP login | 6-digit OTP code in email body |

### 12.2 Mail Configuration

- **Provider:** Gmail SMTP (`smtp.gmail.com:587`) with STARTTLS authentication
- **From address:** `itadmin@tiameds.ai` (default; configurable via `SPRING_MAIL_FROM`)
- **Rate limiting for verification emails:** Max 3 per email address per 60 minutes (`EmailRateLimitService`)
- **Rate limiting for password reset emails:** DB-backed per-email and per-IP limits for cross-instance safety

---

## 13. AWS Infrastructure Architecture

```
                    ┌──────────────────────────────────────────────────────┐
                    │              AWS Cloud  (ap-south-1)                 │
                    │                                                      │
                    │  ┌────────────────────────────────────────────────┐  │
Internet ──────────►│  │           Amazon ECS Fargate                   │  │
                    │  │                                                │  │
                    │  │  Task: tiameds-lab-task-defination              │  │
                    │  │  CPU: 1 vCPU (1024 units)                      │  │
                    │  │  Memory: 3 GB (3072 MB)                        │  │
                    │  │  Network: awsvpc                               │  │
                    │  │  Architecture: X86_64 / Linux                  │  │
                    │  │                                                │  │
                    │  │  Container: tiamed-lab-test:latest             │  │
                    │  │  Image registry: Amazon ECR                    │  │
                    │  │  Port: 8080/tcp                                │  │
                    │  └──────────────────┬─────────────────────────────┘  │
                    │                     │ reads secrets at startup        │
                    │  ┌──────────────────▼─────────────────────────────┐  │
                    │  │  AWS Secrets Manager  (tiamedlabsecret-SM3XcQ) │  │
                    │  │  40+ secrets: DB credentials, JWT keys,        │  │
                    │  │  S3 config, SMTP credentials, rate limit       │  │
                    │  │  config, onboarding URLs, cookie settings      │  │
                    │  └────────────────────────────────────────────────┘  │
                    │                                                      │
                    │  ┌──────────────────┐   ┌──────────────────────┐    │
                    │  │  Amazon RDS      │   │  Amazon S3           │    │
                    │  │  PostgreSQL      │   │  Lab assets          │    │
                    │  │  (ap-south-1)    │   │  (logos, signatures) │    │
                    │  └──────────────────┘   └──────────────────────┘    │
                    │                                                      │
                    │  ┌────────────────────────────────────────────────┐  │
                    │  │  Amazon CloudWatch Logs                        │  │
                    │  │  Log group: /ecs/tiameds-lab-task-defination   │  │
                    │  │  Mode: non-blocking  Buffer: 25 MB             │  │
                    │  └────────────────────────────────────────────────┘  │
                    └──────────────────────────────────────────────────────┘
```

### ECS Task Configuration Summary

| Property | Value |
|---|---|
| Cluster type | AWS Fargate |
| Region | ap-south-1 (Mumbai) |
| CPU | 1024 units (1 vCPU) |
| Memory | 3072 MB (3 GB) |
| Network mode | awsvpc |
| Container port | 8080 |
| Secret injection | AWS Secrets Manager (all 40+ environment variables) |
| Log driver | awslogs (non-blocking, 25 MB buffer) |
| IAM role | `ecsTaskExecutionRole` (ECR pull + Secrets Manager read + CloudWatch write) |
| Runtime | X86_64 / Linux |
| Compatibility | EC2 and Fargate |

---

## 14. CI/CD Pipeline

### 14.1 Active Pipeline (GitHub Actions)

```
Trigger: push or pull_request to main branch  (or manual workflow_dispatch)
    │
    ▼
Checkout code  (actions/checkout@v4)
    │
    ▼
Setup JDK 17  (Temurin, Maven cache)
    │
    ▼
Maven Build — mvn clean package -Pci -Dmaven.test.skip=true
    Profile `ci` skips OWASP dependency-check for speed.
    Tests are universally skipped.
    Output: target/app.jar
    │
    ▼
Configure AWS credentials
    (AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY / AWS_REGION from GitHub Secrets)
    │
    ▼
Amazon ECR login  (amazon-ecr-login@v2)
    │
    ▼
Docker build, tag with git SHA, push to ECR
    docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$GITHUB_SHA .
    docker push ...
    │
    ▼
Update ECS Task Definition
    Render new task definition JSON with the new image tag.
    (amazon-ecs-render-task-definition@v1)
    │
    ▼
Deploy to ECS Service
    (amazon-ecs-deploy-task-definition@v2)
    Waits for service stability before completing.
```

### 14.2 Security Scanning — Status

The following scans exist in the pipeline configuration but are currently **disabled** (commented out or gated by `if: false`) to reduce deployment time:

| Tool | Purpose | Status |
|---|---|---|
| OWASP Dependency-Check 10.0.4 | Third-party CVE vulnerability scanning | Commented out in CI |
| SpotBugs + FindSecBugs 1.13.0 | Static application security testing (SAST) | Commented out in CI |
| SonarQube | Code quality and SAST | `if: false` in pipeline |
| OWASP ZAP Baseline | Dynamic DAST baseline scan | `if: false` in pipeline |
| OWASP ZAP Full Scan | Dynamic DAST full scan | `if: false` in pipeline |
| JetBrains Qodana | Code quality analysis | Separate `qodana_code_quality.yml` workflow |

### 14.3 Docker Image

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
RUN apk update && apk upgrade && apk add --no-cache bash curl
COPY target/app.jar /app/app.jar
EXPOSE 8080
# Non-root user for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
CMD ["java", "-jar", "app.jar"]
```

---

## 15. Configuration & Secrets Management

### 15.1 Spring Profiles

| Profile | Used In | Database |
|---|---|---|
| `dev` | Local development | Local PostgreSQL |
| `test` | ECS deployed test/staging environment | AWS RDS via Secrets Manager |
| `prod` | Production environment | AWS RDS via Secrets Manager |
| `ci` | GitHub Actions CI build | H2 in-memory (OWASP scan skipped) |

### 15.2 Environment Variable Categories

All sensitive values are externalized as environment variables. In ECS Fargate, every variable is injected from AWS Secrets Manager at task startup:

| Category | Key Variables |
|---|---|
| Database | `DB_URL`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` |
| JWT keys | `JWT_PRIVATE_KEY_LOCATION`, `JWT_PUBLIC_KEY_LOCATION` |
| JWT behavior | `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_ACCESS_TOKEN_TTL`, `JWT_REFRESH_TOKEN_TTL`, `JWT_SUBJECT_ENCRYPTION_KEY` |
| JWT cookies | `JWT_ACCESS_COOKIE_NAME`, `JWT_REFRESH_COOKIE_NAME`, `JWT_COOKIE_DOMAIN`, `JWT_COOKIE_PATH`, `JWT_COOKIE_SECURE`, `JWT_COOKIE_SAMESITE` |
| SMTP | `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SPRING_MAIL_SMTP_AUTH`, `SPRING_MAIL_SMTP_STARTTLS_ENABLE` |
| S3 | `AWS_S3_BUCKET`, `AWS_REGION`, `AWS_S3_CDN_BASE_URL`, `AWS_S3_PRESIGN_EXPIRY_MINUTES` |
| Onboarding | `ONBOARDING_TOKEN_EXPIRY_MINUTES`, `ONBOARDING_RATE_MAX_EMAILS_PER_HOUR`, `ONBOARDING_RATE_WINDOW_MINUTES`, `ONBOARDING_FRONTEND_BASE_URL`, `ONBOARDING_VERIFICATION_URL`, `ONBOARDING_ONBOARDING_URL` |
| Password reset | `PASSWORD_RESET_TOKEN_EXPIRY_MINUTES`, `PASSWORD_RESET_TOKEN_LENGTH`, `PASSWORD_RESET_URL`, `PASSWORD_RESET_RATE_LIMIT_EMAIL_MAX`, `PASSWORD_RESET_RATE_LIMIT_EMAIL_WINDOW`, `PASSWORD_RESET_RATE_LIMIT_IP_MAX`, `PASSWORD_RESET_RATE_LIMIT_IP_WINDOW` |

---

## 16. Key Business Flows

### 16.1 Patient Visit Lifecycle

```
1. REGISTER PATIENT
   POST /api/v1/lab/{labId}/add-patient
   PatientService: create PatientEntity; assign patientCode via LabEntitySequence;
   link to lab via lab_patients join table.

2. CREATE VISIT
   POST /api/v1/lab/{labId}/visits
   VisitService: create VisitEntity with visitCode; attach tests/packages via join tables;
   link to lab via lab_visit.

3. ASSOCIATE SAMPLES
   POST /api/v1/lab/add-samples
   SampleAssociationService: create VisitSample join entity for each selected sample type.

4. ENTER TEST RESULTS
   POST /api/v1/lab/{labId}/report
   ReportService: create/update ReportEntity; populate testRows (JSONB); set reference ranges.

5. MARK REPORT COMPLETE
   VisitTestResult.reportStatus = "COMPLETED"
   Triggers RollupRecomputeEvent → DashboardRollupService.recomputeDay() updates daily_lab_stats.

6. GENERATE BILLING
   POST /api/v1/lab/{labId}/billing
   BillingService: create BillingEntity (GST split, discount); create TransactionEntity records
   per payment method (CASH / UPI / CARD); link to visit (1:1) and to lab.
   Triggers RollupRecomputeEvent → revenue stats updated in daily_lab_stats.

7. COMPLETE VISIT
   POST /api/v1/lab/{labId}/complete-visit/{visitId}
   VisitService: set visitStatus = "COMPLETED".
```

### 16.2 Lab Onboarding Flow

```
1. SUBMIT REGISTRATION REQUEST
   POST /api/v1/onboarding/register  {labDetails, adminUser}
   OnboardingService: persist pending lab; send email with verification link
   (token TTL: 15 min; rate limit: 3 emails / 60 min per address).

2. VERIFY EMAIL
   GET /api/v1/onboarding/verify?token=...
   VerificationTokenService: validate token expiry; mark email verified.

3. COMPLETE ONBOARDING FORM
   POST /api/v1/onboarding/complete
   OnboardingService: finalize lab profile; create ADMIN user; activate lab (isActive=true);
   run LabDefaultDataService to seed default tests/samples.
```

### 16.3 Password Reset Flow

```
1. REQUEST RESET
   POST /api/v1/auth/forgot-password  {email}
   PasswordResetRateLimitService: check email rate (3/min) and IP rate (3/min) — DB-backed.
   PasswordResetService: generate cryptographically secure token (32 bytes); store hash in DB.
   EmailService: send reset link to user's email.

2. VALIDATE TOKEN
   GET /api/v1/auth/validate-reset-token?token=...
   PasswordResetService: verify token exists, is not expired (15-min default), is not used.

3. SUBMIT NEW PASSWORD
   POST /api/v1/auth/reset-password  {token, newPassword}
   PasswordValidator: enforce complexity rules.
   BCrypt.encode(newPassword); save to DB.
   Increment user.tokenVersion → immediately invalidates all existing JWT sessions.
   Mark reset token as used.
```

---

## 17. Non-Functional Characteristics

### 17.1 Performance

| Aspect | Mechanism |
|---|---|
| Batch writes | HikariCP autocommit off; Hibernate batch inserts (size 15) with ordered INSERTs/UPDATEs |
| Dashboard queries | Pre-aggregated rollup tables — no live GROUP BY on billing/visit tables for dashboards |
| Connection pooling | HikariCP max 40 connections, max-lifetime 15 minutes |
| Response compression | Server-side GZIP for JSON, XML, HTML, JS, CSS |
| Prepared statement cache | 500 statements, 1024-char SQL limit |
| N+1 prevention | `fail_on_pagination_over_collection_fetch=true`; lazy loading on all collections |
| Audit log writes | Fully asynchronous (`AuditLogService.persistAsync`) — no latency added to business requests |
| In-clause optimization | `in_clause_parameter_padding=true` improves query plan caching for IN queries |
| Query plan cache | `plan_cache_max_size=4096` |

### 17.2 Availability and Resilience

| Aspect | Current State |
|---|---|
| Deployment model | Single ECS Fargate task (no auto-scaling configured in task definition) |
| Stateless design | Fully stateless — no session state; any replica can serve any request |
| Startup resilience | Per-lab backfill failures are caught and logged; they never abort application startup |
| Rollup resilience | `DashboardRollupService` catches all exceptions — rollup failure never fails the business transaction |
| DB failure isolation | Async audit writes cannot cause data loss in the business transaction if DB write fails |

### 17.3 Security Controls Summary

| Control | Implementation |
|---|---|
| Authentication | RS256 JWT in HttpOnly cookies |
| Authorization | Spring Security role-based (`hasAnyRole`) per URL pattern |
| Password storage | BCrypt (Spring Security default rounds) |
| Secret management | AWS Secrets Manager — zero plaintext in Docker image or Git |
| Login rate limiting | Bucket4j in-memory (5 attempts / 10 min per user) |
| Password reset rate limiting | DB-backed per-email and per-IP (cross-instance safe) |
| SQL injection | JPA/Hibernate parameterized queries only; no native SQL string concatenation |
| XSS | Content Security Policy header (`script-src 'self'`) |
| Clickjacking | `X-Frame-Options: SAMEORIGIN` |
| HTTPS enforcement | HSTS with 1-year max-age, includeSubDomains, preload |
| File upload limits | Max 10 MB per file and per request (Spring multipart config) |
| Container hardening | Non-root OS user (`appuser`) in Docker image |
| SAST | SpotBugs + FindSecBugs configured in Maven; not running in active CI pipeline |

### 17.4 Observability

| Aspect | Current State |
|---|---|
| Application logging | SLF4J / Logback via Spring Boot defaults |
| Log shipping | AWS CloudWatch Logs (non-blocking driver; 25 MB buffer) |
| Log levels | `INFO` for application packages; `WARN` for Spring Security and Jackson in production |
| Audit trail | `lab_audit_logs` table — full before/after JSON, field diff, actor identity, IP |
| Metrics | No metrics endpoint — Spring Boot Actuator not in dependencies |
| Distributed tracing | Not configured |
| Error format | Custom JSON error body; Spring whitelabel error page disabled |

---

## 18. Technology Stack Summary

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot | 3.3.4 |
| Web layer | Spring MVC (embedded Tomcat) | — |
| Security | Spring Security | 6.x |
| ORM | Hibernate / Spring Data JPA | 6.x |
| Database | PostgreSQL | — |
| Connection pool | HikariCP | Spring Boot default |
| DB migrations | Flyway (PostgreSQL dialect) | Spring Boot default |
| JWT | JJWT (io.jsonwebtoken) | 0.11.2 |
| Rate limiting | Bucket4j | 7.6.0 |
| AOP | Spring AOP + AspectJ | — |
| Email | Spring Boot Mail / Jakarta Mail | — |
| AWS SDK | AWS SDK for Java v2 (S3, Secrets Manager) | 2.20.0 |
| CSV parsing | Apache Commons CSV | 1.8 |
| API documentation | SpringDoc OpenAPI / Swagger UI | 2.6.0 |
| Code generation | Lombok | 1.18.30 |
| Build tool | Maven | — |
| Containerization | Docker (eclipse-temurin:17-jdk-alpine base) | — |
| Container orchestration | AWS ECS Fargate | — |
| Image registry | Amazon ECR | — |
| CI/CD | GitHub Actions | — |
| SAST | SpotBugs 4.8.6.4 + FindSecBugs 1.13.0 | Configured; not active in CI |
| CVE scanning | OWASP Dependency-Check 10.0.4 | Configured; not active in CI |
| DAST | OWASP ZAP | Configured; not active in CI |
| Code quality | JetBrains Qodana | Separate workflow |

---

## 19. Known Gaps & Recommended Improvements

> These observations are based on the current codebase and represent areas for production hardening, scalability improvement, or maintainability — not defects in the current implementation.

### Security Gaps

| Observation | Recommendation |
|---|---|
| Security scans (OWASP, SpotBugs, SonarQube, ZAP) are commented out of CI | Re-enable on a separate non-blocking security gate so scans run without blocking deployment velocity |
| `ddl-auto=update` used even in production profiles | Switch to `ddl-auto=validate` in production — let Flyway own all schema changes exclusively |
| JWT RSA key files loaded from classpath | In ECS Fargate, inject key material as environment-variable-encoded strings from Secrets Manager rather than embedding in the image |
| `RateLimitFilter` is a pass-through stub | Document clearly or remove to reduce code confusion; the actual limiting is done in controllers |

### Scalability Gaps

| Observation | Recommendation |
|---|---|
| Single ECS task with no horizontal auto-scaling configured | Add ECS Service auto-scaling (target-tracking CPU/memory policy) |
| Bucket4j in-memory rate limiting for login | Will fail under multiple ECS replicas — replace with Redis-backed Bucket4j for distributed rate limiting |
| HikariCP max 40 connections with 1 vCPU | Under multiple replicas each with 40 connections, RDS `max_connections` will be exhausted; use PgBouncer or tune pool size per replica |
| `RollupStartupBackfillRunner` is synchronous and single-threaded | For large databases, move to parallel execution using the existing `RollupAsyncConfig` thread pool |

### Observability Gaps

| Observation | Recommendation |
|---|---|
| No Actuator / Micrometer metrics | Add `spring-boot-starter-actuator` with Micrometer → export to CloudWatch or Prometheus |
| No distributed tracing | Add OpenTelemetry (or Spring Cloud Sleuth) for request correlation, especially across async events |
| `X-Request-ID` is captured in audit logs but not echoed in responses | Return `X-Request-ID` in response headers so clients can correlate errors with audit records |

### Code Quality Gaps

| Observation | Recommendation |
|---|---|
| Tests universally skipped (`maven.test.skip=true`) | Write unit tests for service layer and integration tests for repositories; run them in CI |
| User.roles and User.modules use `FetchType.EAGER` | Roles: acceptable (small set). Modules: switch to LAZY if the set of modules per user grows |
| `@Transient` fields on `ReportEntity` (patientCode, visitCode, createdDateTime, etc.) | These are not persisted — use a dedicated response DTO to prevent serialization ambiguity |
| No caching layer | Apply Spring Cache (Caffeine) on frequently read, rarely changing data: lab info, test catalogue, reference ranges |
| `SuperAdminTestEntity` and `SuperAdminReferenceEntity` master data management | Add an import/seed mechanism with version tracking for the global test catalogue |

---

*This document was reverse-engineered entirely from the actual production codebase of the `tiameds` Spring Boot application and reflects the implementation as of September 2026. No aspirational or planned architecture is described unless explicitly labeled as a recommendation in Section 19.*
