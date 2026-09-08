# TiaMeds Lab Automation Platform — Low Level Design (LLD)

> **Document type:** Low-Level Design (Current State — reverse-engineered from actual codebase)
> **Application:** `tiameds` · `com.tiameds` · Spring Boot 3.3.4 / Java 17
> **Last updated:** September 2026
> **Environment:** AWS ECS Fargate · ap-south-1 · PostgreSQL
> **Companion document:** [HLD.md](HLD.md) — read HLD first for architectural context

---

## Table of Contents

1. [Package Structure](#1-package-structure)
2. [Entity Layer — Full Schema](#2-entity-layer--full-schema)
3. [Repository Layer — Queries & Projections](#3-repository-layer--queries--projections)
4. [Service Layer — Method Signatures & Logic](#4-service-layer--method-signatures--logic)
5. [DTO Catalogue](#5-dto-catalogue)
6. [Controller Layer — Endpoint Details](#6-controller-layer--endpoint-details)
7. [Security Implementation](#7-security-implementation)
8. [Utility Classes](#8-utility-classes)
9. [Configuration Beans](#9-configuration-beans)
10. [Filter Chain — Implementation Detail](#10-filter-chain--implementation-detail)
11. [Event-Driven Rollup — Class-Level Detail](#11-event-driven-rollup--class-level-detail)
12. [Audit Subsystem — Implementation Detail](#12-audit-subsystem--implementation-detail)
13. [Flyway Migration Catalogue](#13-flyway-migration-catalogue)
14. [Application Properties — Full Reference](#14-application-properties--full-reference)
15. [Sequence Diagrams — Key Flows](#15-sequence-diagrams--key-flows)
16. [ID Generation Strategies — Implementation](#16-id-generation-strategies--implementation)
17. [JSON / JSONB Handling](#17-json--jsonb-handling)
18. [Cross-Cutting Design Decisions](#18-cross-cutting-design-decisions)

---

## 1. Package Structure

```
src/main/java/tiameds/com/tiameds/
│
├── audit/
│   ├── Auditable.java                   @interface — marks controller methods for AOP capture
│   ├── AuditAspect.java                 @Around advice — captures before/after state, writes async
│   ├── AuditLogService.java             persistAsync(LabAuditLogs) — @Async DB write
│   └── FieldChangeTracker.java          computeDiff(oldObj, newObj) → Map<field, [old,new]>
│
├── config/
│   ├── SpringSecurityConfig.java        SecurityFilterChain, BCryptPasswordEncoder, AuthManager
│   ├── JwtProperties.java               @ConfigurationProperties("security.jwt") record
│   ├── CorsConfig.java                  CorsConfigurationSource bean
│   ├── RollupAsyncConfig.java           rollupTaskExecutor ThreadPoolTaskExecutor bean
│   ├── IpWhitelistConfig.java           IP whitelist property binding
│   └── SwaggerConfig.java               SpringDoc OpenAPI bean
│
├── controller/
│   ├── admin/
│   │   ├── AdminStatsController.java
│   │   ├── CategoryStatsRollupAdminController.java
│   │   └── AuditLogsController.java
│   ├── auth/
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   ├── ModuleController.java
│   │   └── SecurityController.java
│   ├── lab/
│   │   ├── PatientController.java
│   │   ├── VisitController.java
│   │   ├── BillingController.java
│   │   ├── TestController.java
│   │   ├── HealthPackageController.java
│   │   ├── DoctorController.java
│   │   ├── InsuranceController.java
│   │   ├── LabAdminController.java
│   │   ├── LabMemberController.java
│   │   ├── StaticController.java
│   │   ├── ReportSettingController.java
│   │   ├── AiClinicalObservationController.java
│   │   └── PatientHealthSnapshotController.java
│   ├── labforall/
│   │   └── LabForAll.java
│   ├── onboarding/
│   │   ├── OnboardingController.java
│   │   └── EmailVerificationController.java
│   ├── sampleassociation/
│   │   ├── SampleAssociationController.java
│   │   └── ReportGenerationController.java
│   └── superAdmin/
│       ├── LabSuperAdminController.java
│       ├── SuperAdminDashboardController.java
│       ├── DashboardRollupAdminController.java
│       ├── SuperAdminStatsController.java
│       └── ReferenceAndTestController.java
│
├── dto/
│   ├── auth/                            LoginRequest, AuthResponse, RegisterRequest, etc.
│   ├── lab/                             PatientDTO, VisitDTO, BillingDTO, TestDTO, etc.
│   └── onboarding/                      OnboardingRequestDTO, VerificationResponseDTO, etc.
│
├── entity/                              32 JPA entity classes
│
├── filter/
│   ├── IpWhitelistFilter.java           OncePerRequestFilter — IP allow-list gate
│   ├── RateLimitFilter.java             OncePerRequestFilter — currently pass-through stub
│   └── JwtFilter.java                   OncePerRequestFilter — RS256 JWT validation
│
├── repository/                          34 Spring Data JPA repository interfaces
│
├── services/
│   ├── auth/
│   │   ├── UserService.java
│   │   ├── OtpService.java
│   │   ├── PasswordResetService.java
│   │   ├── PasswordResetRateLimitService.java
│   │   ├── RefreshTokenService.java
│   │   └── MemberUserServices.java
│   ├── email/
│   │   └── EmailService.java
│   ├── lab/
│   │   ├── PatientService.java
│   │   ├── UpdatePatientService.java
│   │   ├── VisitService.java
│   │   ├── BillingService.java
│   │   ├── BillingManagementService.java
│   │   ├── TestServices.java
│   │   ├── TestReferenceServices.java
│   │   ├── ReportService.java
│   │   ├── ReportSettingsService.java
│   │   ├── DoctorService.java
│   │   ├── InsuranceServices.java
│   │   ├── SampleAssociationService.java
│   │   ├── LabService.java
│   │   ├── LabCreationService.java
│   │   ├── LabDefaultDataService.java
│   │   ├── UserLabService.java
│   │   ├── StaticServices.java
│   │   ├── SequenceGeneratorService.java
│   │   ├── S3StorageService.java
│   │   ├── DashboardRollupService.java
│   │   ├── DashboardRollupBackfillService.java
│   │   ├── DashboardRollupVerificationService.java
│   │   ├── CategoryStatsRollupService.java
│   │   ├── CategoryStatsBackfillService.java
│   │   ├── RollupRecomputeEvent.java
│   │   ├── RollupRecomputeListener.java
│   │   └── CategoryStatsRollupListener.java
│   ├── onboarding/
│   │   ├── OnboardingService.java
│   │   ├── VerificationTokenService.java
│   │   └── EmailRateLimitService.java
│   └── superAdmin/
│       └── LabSuperAdminService.java
│
└── utils/
    ├── JwtUtil.java                     RS256 token generation/parsing + AES/GCM subject encryption
    ├── EncryptionUtil.java              AES/GCM/NoPadding utility (12-byte IV, 128-bit tag)
    ├── SnowflakeIdentifierGenerator.java Custom Hibernate ID generator
    ├── PasswordValidator.java           Complexity rule enforcer
    └── EncodingUtils.java               Base64 helpers
```

---

## 2. Entity Layer — Full Schema

### 2.1 `User` — Table: `users`

| Field | Java Type | Column | Constraints | Notes |
|---|---|---|---|---|
| `id` | `Long` | `user_id` | PK, IDENTITY | Auto-increment |
| `username` | `String` | `username` | UNIQUE, NOT NULL | Login handle |
| `password` | `String` | `password` | NOT NULL | BCrypt hash |
| `email` | `String` | `email` | UNIQUE, NOT NULL, @Email | |
| `isVerified` | `boolean` | `is_verified` | NOT NULL | @JsonProperty("verified") |
| `enabled` | `boolean` | `enabled` | NOT NULL | Account active flag |
| `firstName` | `String` | `first_name` | NOT NULL | |
| `lastName` | `String` | `last_name` | NOT NULL | |
| `phone` | `String` | `phone` | NOT NULL | |
| `address` | `String` | `address` | NOT NULL | |
| `city` | `String` | `city` | NOT NULL | |
| `state` | `String` | `state` | NOT NULL | |
| `zip` | `String` | `zip` | NOT NULL | |
| `country` | `String` | `country` | NOT NULL | |
| `tokenVersion` | `Integer` | `token_version` | NOT NULL, default=0 | Increment to invalidate all sessions |
| `userCode` | `String` | `user_code` | UNIQUE | Human-readable user identifier |
| `createdAt` | `LocalDateTime` | `created_at` | NOT NULL, updatable=false | @CreationTimestamp |
| `updatedAt` | `LocalDateTime` | `updated_at` | | @UpdateTimestamp |
| `roles` | `Set<Role>` | — | `users_roles` join table | EAGER fetch; @ManyToMany |
| `modules` | `Set<ModuleEntity>` | — | `users_modules` join table | EAGER fetch; @ManyToMany; @JsonManagedReference |
| `createdBy` | `User` | `created_by` | FK → users | @ManyToOne LAZY; @JsonBackReference |
| `labs` | `Set<Lab>` | — | mappedBy="members" | LAZY; @ManyToMany; @JsonBackReference; no cascade |

**Join tables for User:**
- `users_roles`: `(user_id FK, role_id FK)`
- `users_modules`: `(user_id FK, module_id FK)`

---

### 2.2 `Lab` — Table: `labs`

| Field | Java Type | Column | Constraints | Notes |
|---|---|---|---|---|
| `labId` | `long` | `lab_id` | PK, IDENTITY | |
| `name` | `String` | `name` | NOT NULL | |
| `address` | `String` | `address` | NOT NULL | |
| `city` | `String` | `city` | NOT NULL | |
| `state` | `String` | `state` | NOT NULL | |
| `description` | `String` | `description` | NOT NULL | |
| `isActive` | `Boolean` | `is_active` | NOT NULL | Lab activation flag |
| `labLogo` | `String` | `lab_logo` | nullable | S3 CDN URL |
| `licenseNumber` | `String` | `license_number` | NOT NULL | |
| `labType` | `String` | `lab_type` | NOT NULL | |
| `labZip` | `String` | `lab_zip` | NOT NULL | |
| `labCountry` | `String` | `lab_country` | NOT NULL | |
| `labPhone` | `String` | `lab_phone` | NOT NULL | |
| `labEmail` | `String` | `lab_email` | NOT NULL | |
| `directorName` | `String` | `director_name` | NOT NULL | |
| `directorEmail` | `String` | `director_email` | NOT NULL | |
| `directorPhone` | `String` | `director_phone` | NOT NULL | |
| `certificationBody` | `String` | `certification_body` | NOT NULL | |
| `labCertificate` | `String` | `lab_certificate` | NOT NULL | |
| `directorGovtId` | `String` | `director_govt_id` | NOT NULL | |
| `labBusinessRegistration` | `String` | `lab_business_registration` | NOT NULL | |
| `labLicense` | `String` | `lab_license` | NOT NULL | |
| `taxId` | `String` | `tax_id` | NOT NULL | |
| `labAccreditation` | `String` | `lab_accreditation` | NOT NULL | |
| `dataPrivacyAgreement` | `Boolean` | `data_privacy_agreement` | NOT NULL | |
| `createdAt` | `LocalDateTime` | `created_at` | updatable=false | @CreationTimestamp |
| `updatedAt` | `LocalDateTime` | `updated_at` | | @UpdateTimestamp |
| `createdBy` | `User` | — | FK | @ManyToOne LAZY; @JsonBackReference |
| `members` | `Set<User>` | — | `lab_members` join table | LAZY; @ManyToMany; @JsonManagedReference |
| `tests` | `List<Test>` | — | `lab_tests` join table | LAZY; @ManyToMany; @JsonManagedReference |
| `healthPackages` | `List<HealthPackage>` | — | `lab_packages` join table | LAZY; @ManyToMany; @JsonManagedReference |
| `testReferences` | `List<TestReferenceEntity>` | — | `lab_test_references` join table | LAZY; @ManyToMany; @JsonManagedReference |
| `doctors` | `List<Doctors>` | — | `lab_doctors` join table | LAZY; @ManyToMany; @JsonManagedReference |
| `insurance` | `List<InsuranceEntity>` | — | `lab_insurance` join table | LAZY; @ManyToMany; @JsonManagedReference |

---

### 2.3 `PatientEntity` — Table: `patients`

| Field | Java Type | Column | Constraints | Notes |
|---|---|---|---|---|
| `patientId` | `Long` | `patient_id` | PK, IDENTITY | |
| `firstName` | `String` | `first_name` | | |
| `lastName` | `String` | `last_name` | | |
| `email` | `String` | `email` | | |
| `phone` | `String` | `phone` | | |
| `address` | `String` | `address` | | |
| `city` | `String` | `city` | | |
| `state` | `String` | `state` | | |
| `zip` | `String` | `zip` | | |
| `bloodGroup` | `String` | `Blood_Group` | | |
| `dateOfBirth` | `LocalDate` | `date_of_birth` | | |
| `age` | `String` | `age` | | Stored as string (e.g. "25Y 3M") |
| `gender` | `String` | `gender` | | |
| `guardianId` | `Long` | `guardian_id` | FK → patients | Stores parent patient ID |
| `patientCode` | `String` | `patient_code` | UNIQUE | Human-readable, e.g. "PT-00042" |
| `createdBy` | `String` | `created_by` | | Username string |
| `updatedBy` | `String` | `updated_by` | | |
| `createdAt` | `Instant` | `created_at` | | @CreationTimestamp |
| `updatedAt` | `Instant` | `updated_at` | | @UpdateTimestamp |
| `guardian` | `PatientEntity` | — | Self-ref; @ManyToOne LAZY | insertable=false, updatable=false |
| `visits` | `List<VisitEntity>` | — | mappedBy="patient" | LAZY; cascade=ALL |
| `labs` | `Set<Lab>` | — | `lab_patients` join table | LAZY; @ManyToMany; **NO cascade** |

---

### 2.4 `VisitEntity` — Table: `patient_visits`

| Field | Java Type | Column | Constraints | Notes |
|---|---|---|---|---|
| `visitId` | `Long` | `visit_id` | PK, IDENTITY | |
| `visitDate` | `LocalDate` | `visit_date` | NOT NULL | |
| `visitType` | `String` | `visit_type` | | IN-PATIENT / OUT-PATIENT |
| `visitStatus` | `String` | `visit_status` | | ACTIVE / DISCHARGED / CANCELLED |
| `visitDescription` | `String` | `visit_description` | | |
| `visitCancellationReason` | `String` | `visit_cancellation_reason` | | |
| `visitCancellationDate` | `LocalDate` | `visit_cancellation_date` | | |
| `visitCancellationBy` | `String` | `visit_cancellation_by` | | |
| `visitCancellationTime` | `Instant` | `visit_cancellation_time` | | |
| `visitTime` | `Instant` | `visit_time` | | |
| `visitCode` | `String` | `visit_code` | UNIQUE | Human-readable, e.g. "VS-00117" |
| `createdBy` | `String` | `created_by` | | |
| `updatedBy` | `String` | `updated_by` | | |
| `createdAt` | `Instant` | `created_at` | | @CreationTimestamp |
| `updatedAt` | `Instant` | `updated_at` | | @UpdateTimestamp |
| `patient` | `PatientEntity` | — | nullable=false; @ManyToOne LAZY | |
| `billing` | `BillingEntity` | — | @OneToOne LAZY; cascade=ALL | Owning side via visit |
| `doctor` | `Doctors` | — | nullable; @ManyToOne LAZY | Optional treating doctor |
| `tests` | `List<Test>` | — | `patient_visit_tests` join table | LAZY; @JsonBackReference |
| `packages` | `List<HealthPackage>` | — | `patient_visit_packages` join table | LAZY; @JsonBackReference |
| `insurance` | `List<InsuranceEntity>` | — | `visit_insurance` join table | LAZY; @JsonManagedReference |
| `visitSamples` | `List<VisitSample>` | — | cascade=ALL, orphanRemoval=true | @JsonManagedReference |
| `testResults` | `List<VisitTestResult>` | — | cascade=ALL, orphanRemoval=true | |
| `labs` | `Set<Lab>` | — | `lab_visit` join table | LAZY; @ManyToMany |

---

### 2.5 `BillingEntity` — Table: `billing`

| Field | Java Type | Column | Constraints | Notes |
|---|---|---|---|---|
| `billingId` | `Long` | `billing_id` | PK, IDENTITY | |
| `totalAmount` | `BigDecimal` | `total_amount` | NOT NULL | Pre-discount total |
| `discount` | `BigDecimal` | `discount` | NOT NULL | Total discount amount |
| `gstRate` | `BigDecimal` | `gst_rate` | NOT NULL | GST % applied |
| `gstAmount` | `BigDecimal` | `gst_amount` | NOT NULL | Total GST |
| `cgstAmount` | `BigDecimal` | `cgst_amount` | NOT NULL | Central GST portion |
| `sgstAmount` | `BigDecimal` | `sgst_amount` | NOT NULL | State GST portion |
| `igstAmount` | `BigDecimal` | `igst_amount` | NOT NULL | Integrated GST (inter-state) |
| `netAmount` | `BigDecimal` | `net_amount` | NOT NULL | After discount + GST |
| `paymentStatus` | `String` | `payment_status` | NOT NULL | PAID / PARTIAL / PENDING |
| `paymentMethod` | `String` | `payment_method` | NOT NULL | CASH / UPI / CARD / MIXED |
| `paymentDate` | `String` | `payment_date` | NOT NULL | |
| `discountReason` | `String` | `discount_reason` | | |
| `receivedAmount` | `BigDecimal` | `received_amount` | | Declared by desk |
| `actualReceivedAmount` | `BigDecimal` | `actual_received_amount` | | System-computed from transactions |
| `dueAmount` | `BigDecimal` | `due_amount` | | Outstanding balance |
| `packageAmt` | `BigDecimal` | `package_amt` | | Revenue from packages |
| `packageDiscount` | `BigDecimal` | `package_discount` | | Discount from packages |
| `billingTime` | `LocalTime` | `billing_time` | NOT NULL | |
| `billingDate` | `String` | `billing_date` | | |
| `billingCode` | `String` | `billing_code` | UNIQUE | e.g. "BL-00089" |
| `createdBy` | `String` | `created_by` | | |
| `updatedBy` | `String` | `updated_by` | | |
| `createdAt` | `Instant` | `created_at` | | @CreationTimestamp |
| `updatedAt` | `Instant` | `updated_at` | | @UpdateTimestamp |
| `visit` | `VisitEntity` | — | mappedBy="billing"; @OneToOne LAZY | Inverse side |
| `labs` | `List<Lab>` | — | `lab_billing` join table | LAZY; @ManyToMany |
| `transactions` | `List<TransactionEntity>` | — | cascade=ALL, orphanRemoval=true | @JsonManagedReference |
| `testDiscounts` | `List<TestDiscountEntity>` | — | cascade=ALL, orphanRemoval=true | @JsonManagedReference |

---

### 2.6 `Test` — Table: `tests`

| Field | Java Type | Column | Constraints |
|---|---|---|---|
| `testId` | `long` | `test_id` | PK, IDENTITY |
| `category` | `String` | `category` | NOT NULL |
| `name` | `String` | `name` | NOT NULL |
| `price` | `BigDecimal` | `price` | NOT NULL |
| `testCode` | `String` | `test_code` | UNIQUE |
| `createdAt` | `LocalDateTime` | `created_at` | @CreationTimestamp |
| `updatedAt` | `LocalDateTime` | `updated_at` | @UpdateTimestamp |

**Relationships:** `@ManyToMany` mappedBy on Lab, HealthPackage, VisitEntity (all `@JsonBackReference`, LAZY).

---

### 2.7 `TestReferenceEntity` — Table: `test_reference`

| Field | Java Type | Column / Annotation | Notes |
|---|---|---|---|
| `testReferenceId` | `Long` | PK; `@GeneratedValue(generator="snowflake-id")` | Custom `SnowflakeIdentifierGenerator` |
| `category` | `String` | NOT NULL | |
| `testName` | `String` | NOT NULL | |
| `testDescription` | `String` | NOT NULL | |
| `units` | `String` | | |
| `gender` | `Gender` | `@Convert(GenderConverter.class)` | Enum persisted as String |
| `minReferenceRange` | `Double` | | |
| `maxReferenceRange` | `Double` | | |
| `ageMin` | `Integer` | NOT NULL | |
| `ageMax` | `Integer` | NOT NULL | |
| `minAgeUnit` | `AgeUnit` | `@Enumerated(STRING)` | DAYS / MONTHS / YEARS |
| `maxAgeUnit` | `AgeUnit` | `@Enumerated(STRING)` | |
| `testReferenceCode` | `String` | UNIQUE | e.g. "TR-00012" |
| `createdBy` | `String` | NOT NULL | |
| `updatedBy` | `String` | NOT NULL | |
| `createdAt` | `LocalDateTime` | @CreationTimestamp | |
| `updatedAt` | `LocalDateTime` | @UpdateTimestamp | |
| `reportJson` | `String` | `@JdbcTypeCode(JSON)` | Report structure template (JSONB) |
| `referenceRanges` | `String` | `@JdbcTypeCode(JSON)` | Age/gender-specific ranges (JSONB) |
| `dropdown` | `String` | `@JdbcTypeCode(JSON)` | UI dropdown definitions (JSONB) |
| `impression` | `String` | `@JdbcTypeCode(JSON)` | Clinical impression template (JSONB) |

---

### 2.8 `ReportEntity` — Table: `lab_report`

| Field | Java Type | Column | Notes |
|---|---|---|---|
| `reportId` | `Long` | `report_id` PK IDENTITY | |
| `visitId` | `Long` | `visit_id` NOT NULL | FK — no JPA relation to avoid cascade issues |
| `testName` | `String` | `test_name` NOT NULL | |
| `testCategory` | `String` | `test_category` NOT NULL | |
| `patientName` | `String` | `patient_name` NOT NULL | |
| `labId` | `Long` | `lab_id` NOT NULL | |
| `referenceDescription` | `String` | `reference_description` | |
| `referenceRange` | `String` | `reference_range` | |
| `referenceAgeRange` | `String` | `reference_age_range` | |
| `enteredValue` | `String` | `entered_value` length=1000 | |
| `unit` | `String` | `unit` length=4000 | |
| `description` | `String` | `description` length=4000 | |
| `remarks` | `String` | `remarks` length=300 | |
| `comments` | `String` | `comments` length=500 | |
| `createdBy` | `Long` | `created_by` | |
| `updatedBy` | `Long` | `updated_by` | |
| `reportCode` | `String` | `report_code` UNIQUE | |
| `createdAt` | `Instant` | @CreationTimestamp | |
| `updatedAt` | `Instant` | @UpdateTimestamp | |
| `reportJson` | `String` | @JdbcTypeCode(JSON) | Full rendered report payload (JSONB) |
| `referenceRanges` | `String` | @JdbcTypeCode(JSON) | Dynamic reference range data (JSONB) |
| `testRows` | `List<TestRow>` | @JdbcTypeCode(JSON) | Multi-row test results (JSONB) |
| `patientCode` | `String` | **@Transient** | Not persisted — derived for response |
| `visitCode` | `String` | **@Transient** | Not persisted — derived for response |
| `createdDateTime` | `String` | **@Transient** | Not persisted — formatted timestamp |
| `registeredDateTime` | `String` | **@Transient** | Not persisted |
| `sampleCollectedDateTime`| `String` | **@Transient** | Not persisted |

---

### 2.9 `DailyLabStats` — Table: `daily_lab_stats` (Composite Key)

```java
@Entity @Table(name = "daily_lab_stats")
@IdClass(DailyLabStatsId.class)
public class DailyLabStats {
    @Id Long labId;          // column: lab_id
    @Id LocalDate statDate;  // column: stat_date

    Long testCount;          // default 0
    Long reportsGenerated;   // default 0
    Long pendingSamples;     // default 0
    Long patientCount;       // default 0
    BigDecimal paidRevenue;  // default 0
    BigDecimal dueRevenue;   // default 0
    LocalDateTime updatedAt; // column: updated_at  (default now())
}

// Composite PK class:
public class DailyLabStatsId implements Serializable {
    Long labId;
    LocalDate statDate;
}
```

---

### 2.10 `DailyLabCategoryStats` — Table: `daily_lab_category_stats` (Composite Key)

```java
@Entity @Table(name = "daily_lab_category_stats")
@IdClass(DailyLabCategoryStatsId.class)
public class DailyLabCategoryStats {
    @Id Long labId;
    @Id LocalDate statDate;
    @Id String category;

    Long testCount;
    BigDecimal grossRevenue;   // total before discount
    BigDecimal discount;
    BigDecimal paidRevenue;
    BigDecimal dueRevenue;
    BigDecimal cashRevenue;
    BigDecimal upiRevenue;
    BigDecimal cardRevenue;
    LocalDateTime updatedAt;
}
```

---

### 2.11 `VisitTestResult` — Table: `visit_test_result`

| Field | Java Type | Column | Notes |
|---|---|---|---|
| `id` | `Long` | PK IDENTITY | |
| `isFilled` | `Boolean` | `is_filled` default=false | Whether result has been entered |
| `reportStatus` | `String` | `report_status` | PENDING / COMPLETED |
| `testStatus` | `String` | `test_status` | ACTIVE / CANCELLED / COMPLETED |
| `visitTestResultCode` | `String` | `visit_test_result_code` UNIQUE | |
| `createdBy` / `updatedBy` | `String` | | |
| `createdAt` / `updatedAt` | `Instant` | @CreationTimestamp / @UpdateTimestamp | |
| `visit` | `VisitEntity` | nullable=false; @ManyToOne LAZY | |
| `test` | `Test` | nullable=false; @ManyToOne LAZY | |

---

### 2.12 `LabAuditLogs` — Table: `lab_audit_logs`

| Field | Java Type | Column | Notes |
|---|---|---|---|
| `id` | `UUID` | PK `@GeneratedValue` | Global uniqueness; `@Type(PostgreSQLUUIDType.class)` |
| `timestamp` | `LocalDateTime` | | Event time |
| `userId` | `Long` | | Acting user ID |
| `username` | `String` | | Acting user's username |
| `role` | `String` | | User's primary role |
| `labId` | `String` | `lab_id` | Lab context |
| `ipAddress` | `String` | `ip_address` | X-Forwarded-For or remote addr |
| `deviceInfo` | `String` | `device_info` | User-Agent header |
| `requestId` | `String` | `request_id` | X-Request-ID header |
| `module` | `String` | | e.g. PATIENT, BILLING |
| `entityType` | `String` | `entity_type` | e.g. Patient, Visit |
| `entityId` | `String` | `entity_id` | The affected record ID |
| `actionType` | `String` | `action_type` | CREATE / UPDATE / DELETE / VIEW |
| `oldValue` | `String` | `old_value` @JdbcTypeCode(JSON) | Before-state JSON (JSONB) |
| `newValue` | `String` | `new_value` @JdbcTypeCode(JSON) | After-state JSON (JSONB) |
| `fieldChanged` | `String` | `field_changed` @JdbcTypeCode(JSON) | Field diff JSON (JSONB) |
| `changeReason` | `String` | `change_reason` TEXT | |
| `severity` | `Enum` | | LOW / MEDIUM / HIGH / CRITICAL |

---

### 2.13 `LabEntitySequence` — Table: `lab_entity_sequence`

```java
@Entity @Table(name = "lab_entity_sequence")
@IdClass(LabEntitySequenceId.class)
public class LabEntitySequence {
    @Id Long labId;         // column: lab_id
    @Id String entityType;  // column: entity_type  (e.g. "PATIENT", "VISIT", "BILLING")
    Long currentValue;      // current counter, incremented per-lab per-type
}
```

Codes are generated as `<PREFIX>-<zero-padded-counter>`, e.g. `PT-00042`, `VS-00117`, `BL-00089`.

---

### 2.14 Other Entities (Summary)

| Entity | Table | Key Fields |
|---|---|---|
| `Role` | `roles` | `role_id` INT PK, `name` UNIQUE NOT NULL |
| `ModuleEntity` | `modules` | `module_id` BIGINT PK, `name` UNIQUE NOT NULL |
| `SampleEntity` | `sample_entity` | `sample_id` PK, `name`, `sample_code` UNIQUE, `lab_id` NOT NULL |
| `VisitSample` | `patient_visit_sample` | `id` PK, `visit` FK, `sample` FK, `visit_sample_code` UNIQUE |
| `Doctors` | `doctors` | `doctor_id` PK, `name`, `email`, `speciality`, `doctor_code` UNIQUE |
| `HealthPackage` | `health_packages` | `package_id` PK, `packageName`, `price`, `discount`, `is_active` BOOL default TRUE |
| `InsuranceEntity` | `insurance` | `insurance_id` PK |
| `TransactionEntity` | `billing_transaction` | `transaction_id` PK; `paymentMethod` (CASH/UPI/CARD); `transaction_code` UNIQUE |
| `TestDiscountEntity` | `test_discounts` | Per-test discount overrides within a billing |
| `Otp` | `otp` | OTP code, expiry, user link |
| `RefreshToken` | `refresh_tokens` | `id` UUID PK, token value, user FK, expiry |
| `PasswordResetToken` | `password_reset_tokens` | Hashed token, expiry, `isUsed` flag |
| `PasswordResetRateLimit` | `password_reset_rate_limits` | Per-email and per-IP attempt counts with window |
| `VerificationToken` | `verification_tokens` | Token, expiry, email target |
| `ReportSettings` | `report_settings` | Per-lab report appearance (logo, header, footer) |
| `ReportRoleSetting` | `report_role_settings` | Per-role visibility rules for report fields |
| `SuperAdminTestEntity` | `super_admin_test` | Global test master data (SUPERADMIN-managed) |
| `SuperAdminReferenceEntity` | `super_admin_reference` | Global reference master data |
| `AiClinicalObservation` | `ai_clinical_observations` | AI-generated notes per visit |

---

## 3. Repository Layer — Queries & Projections

### 3.1 `LabRepository` extends `JpaRepository<Lab, Long>`

```java
// Custom queries
@Query("SELECT l FROM Lab l WHERE l.createdBy = :currentUser AND l.isActive = true")
List<Lab> findByCreatedBy(@Param("currentUser") User currentUser);

@Query("SELECT l FROM Lab l JOIN FETCH l.members WHERE l.id = :id")
Optional<Lab> findLabWithMembers(@Param("id") long id);

@Query("SELECT l FROM Lab l JOIN l.members m WHERE m.id = :userId")
Set<Lab> findLabsByUserId(@Param("userId") Long userId);

// Cross-lab performance summary (used by SUPERADMIN dashboard)
// Returns LabPerformanceSummaryProjection with:
//   labId, labName, revenue, previousRevenue, testCount,
//   patientCount, pendingSamples, reportsGenerated, avgTatHours
List<LabPerformanceSummaryProjection> getLabPerformanceSummary(...);

// Native SQL with date-range rollup by lab (pagination supported)
Page<LabPerformanceSummaryProjection> getLabWiseRollupWithDateRange(..., Pageable pageable);
```

**Projection interface — `LabPerformanceSummaryProjection`:**

```java
interface LabPerformanceSummaryProjection {
    Long getLabId();
    String getLabName();
    BigDecimal getRevenue();
    BigDecimal getPreviousRevenue();
    Long getTestCount();
    Long getPatientCount();
    Long getPendingSamples();
    Long getReportsGenerated();
    Double getAvgTatHours();
}
```

---

### 3.2 `UserRepository` extends `CrudRepository<User, Long>`

```java
@Query("SELECT u FROM User u WHERE u.username = :username")
User getUserByUsername(@Param("username") String username);

Optional<User> findByUsername(String username);
Optional<User> findByEmail(String email);
boolean existsByUsername(String username);
boolean existsByEmail(String email);
List<User> findByCreatedBy(User createdBy);
List<User> findByLabsId(Long labId);   // derived query — users belonging to a lab

// Counts active members by role across all labs created by a user:
@Query("SELECT l.id AS labId, l.name AS labName, COUNT(u.id) AS count " +
       "FROM Lab l JOIN l.members u JOIN u.roles r " +
       "WHERE l.createdBy = :createdBy AND r.name = :roleName AND u.enabled = true " +
       "GROUP BY l.id, l.name")
List<LabRoleCountProjection> countRolesByLabsCreatedBy(
    @Param("createdBy") User createdBy, @Param("roleName") String roleName);
```

**Projection — `LabRoleCountProjection`:** `{ Long getLabId(); String getLabName(); Long getCount(); }`

---

### 3.3 `PatientRepository` extends `JpaRepository<PatientEntity, Long>`

```java
@Query("SELECT COUNT(p) FROM PatientEntity p JOIN p.labs l WHERE l.id = :labId")
long countByLabId(@Param("labId") Long labId);

@Query("SELECT COUNT(p) FROM PatientEntity p JOIN p.labs l " +
       "WHERE l.id = :labId AND p.createdAt BETWEEN :startDate AND :endDate")
long countByLabIdAndCreatedAtBetween(Long labId, Instant startDate, Instant endDate);

// Gender distribution (native SQL, groups by normalized gender string):
@Query(value = "SELECT LOWER(p.gender) AS gender, COUNT(*) AS count " +
               "FROM patients p JOIN lab_patients pl ON p.patient_id = pl.patient_id " +
               "WHERE pl.lab_id = :labId AND p.gender IS NOT NULL GROUP BY LOWER(p.gender)",
       nativeQuery = true)
List<GenderCountProjection> countByGenderForLab(@Param("labId") Long labId);

// Age-group distribution (0-18, 19-35, 36-50, 51-65, 65+):
List<AgeGroupCountProjection> countByAgeGroupForLab(@Param("labId") Long labId);
```

---

### 3.4 `VisitRepository` extends `JpaRepository<VisitEntity, Long>`

```java
@Modifying @Transactional
@Query("UPDATE VisitEntity v SET v.visitStatus = :status WHERE v.visitId = :visitId")
int updateVisitStatus(Long visitId, String status);

long countByLabIdAndCreatedAtBetween(Long labId, Instant startDate, Instant endDate);

// Counts distinct patients who had a visit in a lab for a date range:
@Query("SELECT COUNT(DISTINCT v.patient.patientId) FROM VisitEntity v " +
       "JOIN v.labs l WHERE l.id = :labId AND v.createdAt BETWEEN :startDate AND :endDate")
long countDistinctPatientsByLabIdAndCreatedAtBetween(Long labId, Instant start, Instant end);

// Counts visits in PENDING status (pending samples proxy):
long countPendingVisitsByLabIdAndCreatedAtBetween(Long labId, Instant start, Instant end);
```

---

### 3.5 `BillingRepository` extends `JpaRepository<BillingEntity, Long>`

```java
// Revenue aggregation (excludes CANCELLED visits):
@Query("SELECT COALESCE(SUM(b.actualReceivedAmount), 0) FROM BillingEntity b " +
       "JOIN b.labs l WHERE l.id = :labId " +
       "AND b.createdAt BETWEEN :startDate AND :endDate " +
       "AND LOWER(b.visit.visitStatus) != 'cancelled'")
BigDecimal sumPaidAmountByLabId(Long labId, Instant startDate, Instant endDate);

BigDecimal sumDueAmountByLabIdAndCreatedAtBetween(Long labId, Instant start, Instant end);

// Top-8 labs by revenue (native SQL, used by SUPERADMIN):
@Query(value = "SELECT l.name AS labName, COALESCE(bill_agg.revenue, 0) AS revenue, " +
               "COALESCE(bill_agg.discount, 0) AS discount, " +
               "COALESCE(pkg_agg.packageRevenue, 0) AS packageRevenue " +
               "FROM labs l LEFT JOIN (...) bill_agg ... " +
               "WHERE l.created_by = :createdById ORDER BY revenue DESC LIMIT 8",
       nativeQuery = true)
List<RevenueByLabProjection> getRevenueByLab(Long createdById, ...);

// Paginated billing grid report (native SQL, ~15 columns):
@Query(value = GRID_SELECT + "WHERE l.created_by = :createdById ORDER BY b.created_at DESC",
       countQuery = GRID_COUNT, nativeQuery = true)
Page<GridReportRowProjection> getGridReport(Long createdById, ..., Pageable pageable);

// Paginated billings filtered by payment date range:
Page<BillingEntity> findBillingsByLabAndPaymentDateRange(
    Long labId, String startDate, String endDate, Pageable pageable);
```

**Key projections:**

| Projection | Fields |
|---|---|
| `DailyRevenueProjection` | date, revenue |
| `RevenueByLabProjection` | labName, revenue, discount, packageRevenue |
| `DetailedBillingSummaryProjection` | totalBillings, grossRevenue, totalDiscount, totalGst, netRevenue, totalPaid, totalDue, totalCash, totalUpi, totalCard |
| `BillingByStatusProjection` | status, billingCount, grossRevenue, totalDiscount, totalGst, netRevenue, totalPaid, totalDue |
| `RevenueByCollectionMethodProjection` | totalCash, totalUpi, totalCard, totalCredit |
| `GridReportRowProjection` | billingId, billingCode, visitId, visitCode, visitDate, visitStatus, visitType, patientId, patientName, patientPhone, patientCode, labId, labName, doctorName, totalAmount, discount, netAmount, paidAmount, dueAmount, paymentMethod, paymentStatus, billingDate, createdAt |

---

### 3.6 `DailyLabStatsRepository` extends `JpaRepository<DailyLabStats, DailyLabStatsId>`

```java
// Core upsert — PostgreSQL ON CONFLICT syntax:
@Modifying @Transactional
@Query(value = "INSERT INTO daily_lab_stats " +
               "(lab_id, stat_date, test_count, reports_generated, pending_samples, " +
               " patient_count, paid_revenue, due_revenue, updated_at) " +
               "VALUES (:labId, :statDate, :testCount, :reportsGenerated, :pendingSamples, " +
               "        :patientCount, :paidRevenue, :dueRevenue, NOW()) " +
               "ON CONFLICT (lab_id, stat_date) DO UPDATE SET " +
               "  test_count = EXCLUDED.test_count, " +
               "  reports_generated = EXCLUDED.reports_generated, " +
               "  pending_samples = EXCLUDED.pending_samples, " +
               "  patient_count = EXCLUDED.patient_count, " +
               "  paid_revenue = EXCLUDED.paid_revenue, " +
               "  due_revenue = EXCLUDED.due_revenue, " +
               "  updated_at = NOW()",
       nativeQuery = true)
void upsertRow(Long labId, LocalDate statDate, Long testCount, Long reportsGenerated,
               Long pendingSamples, Long patientCount,
               BigDecimal paidRevenue, BigDecimal dueRevenue);

List<DailyLabStats> findByLabIdAndStatDateBetween(Long labId, LocalDate start, LocalDate end);

// Aggregate over date range (used by admin stats dashboard):
@Query("SELECT COALESCE(SUM(d.paidRevenue), 0) AS paidRevenue, " +
       "COALESCE(SUM(d.testCount), 0) AS testCount, ... " +
       "FROM DailyLabStats d WHERE d.labId = :labId AND d.statDate BETWEEN :start AND :end")
RangeSummaryProjection sumRangeForLab(Long labId, LocalDate start, LocalDate end);
```

**Projection — `RangeSummaryProjection`:** `{ testCount, reportsGenerated, pendingSamples, patientCount, paidRevenue, dueRevenue }`

---

### 3.7 `DailyLabCategoryStatsRepository`

```java
@Modifying @Transactional
@Query(value = "INSERT INTO daily_lab_category_stats " +
               "(lab_id, stat_date, category, test_count, gross_revenue, discount, " +
               " paid_revenue, due_revenue, cash_revenue, upi_revenue, card_revenue, updated_at) " +
               "VALUES (...) ON CONFLICT (lab_id, stat_date, category) DO UPDATE SET ...",
       nativeQuery = true)
void upsertRow(Long labId, LocalDate statDate, String category, Long testCount, ...);

// Delete stale category rows before re-inserting (called from CategoryStatsRollupService):
@Modifying @Transactional
void deleteByLabIdAndStatDate(Long labId, LocalDate statDate);

// Aggregate by category across a date range:
@Query("SELECT d.category AS category, SUM(d.testCount) AS testCount, " +
       "SUM(d.grossRevenue) AS grossRevenue, ... " +
       "FROM DailyLabCategoryStats d " +
       "WHERE d.labId = :labId AND d.statDate BETWEEN :start AND :end " +
       "GROUP BY d.category")
List<CategorySummaryProjection> sumRangeByCategoryForLab(Long labId, LocalDate start, LocalDate end);
```

**Projection — `CategorySummaryProjection`:** `{ category, testCount, grossRevenue, discount, paidRevenue, dueRevenue, cashRevenue, upiRevenue, cardRevenue }`

---

### 3.8 `VisitTestResultRepository`

```java
long countAllTestsByLabIdAndCreatedAtBetween(Long labId, Instant start, Instant end);
long countCompletedReportsByLabIdAndCreatedAtBetween(Long labId, Instant start, Instant end);

// Used by CategoryStatsRollupService — returns per-category test counts and revenue:
List<CategoryDetailProjection> getPatientTestsByCategoryDetailedByLabIdWithDateRange(
    Long labId, Instant start, Instant end);
```

---

## 4. Service Layer — Method Signatures & Logic

### 4.1 `VisitService`

```java
@Service @Transactional
public class VisitService {

    // Creates a new visit and its associated billing record.
    // Assigns visitCode (via SequenceGeneratorService) and billingCode.
    // Links tests, packages, insurance, doctor.
    // Publishes RollupRecomputeEvent after flush.
    public void addVisit(Long labId, Long patientId, VisitDTO visitDTO,
                         Optional<User> currentUser);

    // Returns all visits for a lab with patient and test details.
    public List<PatientDTO> getVisits(Long labId, Optional<User> currentUser);
}
```

**Internal flow of `addVisit`:**
1. Validate lab and patient membership.
2. Create `VisitEntity`, assign `visitCode` from `SequenceGeneratorService`.
3. Set tests, packages, insurance, doctor from IDs in `VisitDTO`.
4. Create `BillingEntity` from `BillingDTO`; assign `billingCode`.
5. Set `visit.billing = billing`.
6. Persist via `visitRepository.save(visit)`.
7. Publish `RollupRecomputeEvent(labId, today)`.

---

### 4.2 `DashboardRollupService`

```java
@Service
public class DashboardRollupService {

    // Full re-aggregation for (labId, date). Idempotent — safe to call multiple times.
    // Catches all exceptions; rollup failure never propagates.
    public void recomputeDay(Long labId, LocalDate date) {
        try {
            Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end   = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            BigDecimal paidRevenue   = billingRepository.sumPaidAmountByLabId(labId, start, end);
            BigDecimal dueRevenue    = billingRepository.sumDueAmountByLabIdAndCreatedAtBetween(labId, start, end);
            Long testCount           = visitTestResultRepository.countAllTestsByLabIdAndCreatedAtBetween(labId, start, end);
            Long reportsGenerated    = visitTestResultRepository.countCompletedReportsByLabIdAndCreatedAtBetween(labId, start, end);
            Long pendingSamples      = visitRepository.countPendingVisitsByLabIdAndCreatedAtBetween(labId, start, end);
            Long patientCount        = visitRepository.countDistinctPatientsByLabIdAndCreatedAtBetween(labId, start, end);

            dailyLabStatsRepository.upsertRow(labId, date, testCount, reportsGenerated,
                                              pendingSamples, patientCount, paidRevenue, dueRevenue);
        } catch (Exception e) {
            logger.error("Rollup failed for lab {} on {}: {}", labId, date, e.getMessage(), e);
            // Exception swallowed — business transaction is unaffected.
        }
    }
}
```

---

### 4.3 `CategoryStatsRollupService`

```java
@Service
public class CategoryStatsRollupService {

    // Deletes stale category rows for (labId, date) then re-inserts from source.
    // Catches all exceptions; never propagates.
    public void recomputeDay(Long labId, LocalDate date) {
        try {
            Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end   = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            List<CategoryDetailProjection> rows =
                visitTestResultRepository
                    .getPatientTestsByCategoryDetailedByLabIdWithDateRange(labId, start, end);

            dailyLabCategoryStatsRepository.deleteByLabIdAndStatDate(labId, date);

            for (CategoryDetailProjection row : rows) {
                dailyLabCategoryStatsRepository.upsertRow(
                    labId, date, row.getCategory(),
                    row.getTestCount(), row.getGrossRevenue(), row.getDiscount(),
                    row.getPaidRevenue(), row.getDueRevenue(),
                    row.getCashRevenue(), row.getUpiRevenue(), row.getCardRevenue());
            }
        } catch (Exception e) {
            logger.error("Category rollup failed for lab {} on {}", labId, date, e);
        }
    }
}
```

---

### 4.4 `RollupStartupBackfillRunner`

```java
@Component
public class RollupStartupBackfillRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        List<Lab> labs = labRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Lab lab : labs) {
            try {
                LocalDate startDate = (lab.getCreatedAt() != null)
                    ? lab.getCreatedAt().toLocalDate()
                    : today;

                dashboardRollupBackfillService.backfillLab(lab.getLabId(), startDate, today);
                categoryStatsBackfillService.backfillLab(lab.getLabId(), startDate, today);

            } catch (Exception e) {
                // Per-lab failure is logged; other labs continue.
                logger.error("Startup backfill failed for lab {}", lab.getLabId(), e);
            }
        }
    }
}
```

---

### 4.5 `SequenceGeneratorService`

```java
@Service
public class SequenceGeneratorService {

    // Thread-safe per-lab, per-type sequence increment.
    // Uses @Lock(PESSIMISTIC_WRITE) on LabEntitySequence row.
    @Transactional
    public String generateCode(Long labId, String entityType, String prefix) {
        LabEntitySequence seq = labEntitySequenceRepository
            .findByLabIdAndEntityTypeWithLock(labId, entityType)
            .orElseGet(() -> new LabEntitySequence(labId, entityType, 0L));

        seq.setCurrentValue(seq.getCurrentValue() + 1);
        labEntitySequenceRepository.save(seq);

        return String.format("%s-%05d", prefix, seq.getCurrentValue());
        // Example output: "PT-00042", "VS-00117", "BL-00089"
    }
}
```

---

### 4.6 `PasswordResetService`

```java
@Service
public class PasswordResetService {

    // Generates a cryptographically secure reset token (32 bytes → 64 hex chars).
    // Stores SHA-256 hash in DB; sends plain token in email.
    public void initiatePasswordReset(String email) {
        byte[] tokenBytes = new byte[tokenLength]; // from config, default 32
        secureRandom.nextBytes(tokenBytes);
        String plainToken = HexUtils.toHex(tokenBytes);
        String hashedToken = DigestUtils.sha256Hex(plainToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(hashedToken);
        resetToken.setExpiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES));
        resetToken.setIsUsed(false);
        resetToken.setUser(user);
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), plainToken);
    }

    // Validates token (not expired, not used), then:
    // 1. BCrypt-encodes new password and saves.
    // 2. Increments user.tokenVersion → invalidates all JWT sessions.
    // 3. Marks token as used.
    @Transactional
    public void resetPassword(String plainToken, String newPassword);
}
```

---

### 4.7 `OtpService`

```java
@Service
public class OtpService {

    // Generates 6-digit numeric OTP; stores with TTL; sends via email.
    public void sendOtp(String username);

    // Validates OTP (matches + not expired); on success issues JWT cookies same as login.
    public LoginResponse verifyOtp(String username, String otpCode);
}
```

---

### 4.8 `S3StorageService`

```java
@Service
public class S3StorageService {

    // Uploads file to S3 bucket and returns CDN URL.
    public String upload(String keyPrefix, MultipartFile file);

    // Generates a pre-signed GET URL valid for `presignExpiryMinutes` (default 10).
    public String generatePresignedUrl(String s3Key);
}
```

---

## 5. DTO Catalogue

### 5.1 Auth DTOs

| DTO Class | Key Fields | Direction |
|---|---|---|
| `LoginRequest` | `username`, `password` | Request |
| `AuthResponse` | `userId`, `username`, `email`, `roles`, `modules`, `labIds` | Response |
| `RegisterRequest` | `username`, `password`, `email`, `firstName`, `lastName`, `phone`, … | Request |
| `ForgotPasswordRequest` | `email` | Request |
| `ResetPasswordRequest` | `token`, `newPassword` | Request |
| `SendOtpRequest` | `username` | Request |
| `VerifyOtpRequest` | `username`, `otpCode` | Request |
| `MemberRegisterDto` | Full user fields + `roleId`, `labId` | Request |
| `MemberDetailsUpdate` | Subset of user fields | Request |

---

### 5.2 Lab DTOs

| DTO Class | Key Fields | Notes |
|---|---|---|
| `PatientDTO` | All patient fields + `labId`, `visits` list | Can hold nested visit list |
| `VisitDTO` | `visitId`, `visitDate`, `visitType`, `visitStatus`, `testIds`, `packageIds`, `insuranceIds`, `doctorId`, `billing` (BillingDTO), `@JsonProperty("listofeachtestdiscount")` testDiscounts, `@JsonProperty("testResult")` results | Bidirectional: both request and response |
| `BillingDTO` | All billing financial fields + `transactions` list | |
| `TestDTO` | `testId`, `category`, `name`, `price`, `testCode` | |
| `HealthPackageRequest` | `packageName`, `price`, `discount`, `testIds` | |
| `DoctorDTO` | Doctor fields | |
| `InsuranceDTO` | Insurance fields | |
| `ModuleDTO` | `moduleId`, `name` | |
| `SampleDTO` | `sampleId`, `name`, `sampleCode`, `labId` | |
| `VisitTestResultResponseDTO` | `id`, `testId`, `testName`, `isFilled`, `reportStatus`, `testStatus` | Nested in VisitDTO |
| `TestDiscountDTO` | `testId`, `discountAmount` | Nested in VisitDTO |

---

### 5.3 Onboarding DTOs

| DTO Class | Key Fields |
|---|---|
| `OnboardingRequestDTO` | Full lab details + admin user details |
| `OnboardingResponseDTO` | `message`, `labId`, `verificationEmailSent` |
| `EmailRequestDTO` | `email` (for re-send verification) |
| `VerificationResponseDTO` | `verified`, `message`, `redirectUrl` |

---

## 6. Controller Layer — Endpoint Details

### 6.1 `AuthController` — `/api/v1/auth`

| Method | Path | Request Body | Response | Auth |
|---|---|---|---|---|
| POST | `/login` | `LoginRequest` | `AuthResponse` + Set-Cookie | Public |
| POST | `/register` | `RegisterRequest` | `AuthResponse` | Public |
| POST | `/send-otp` | `SendOtpRequest` | 200 OK | Public |
| POST | `/verify-otp` | `VerifyOtpRequest` | `AuthResponse` + Set-Cookie | Public |
| POST | `/refresh` | — (reads refreshToken cookie) | New accessToken cookie | Public |
| POST | `/forgot-password` | `ForgotPasswordRequest` | 200 OK | Public |
| GET | `/validate-reset-token` | `?token=` | 200 / 400 | Public |
| POST | `/reset-password` | `ResetPasswordRequest` | 200 OK | Public |
| POST | `/logout` | — | Clears cookies | Authenticated |

**Rate limiting on `/login`:** Bucket4j in-memory token bucket — 5 attempts per 10-minute window per username. Configured via `rate.limit.login.attempts=5` and `rate.limit.login.window=10`.

---

### 6.2 `VisitController` — `/api/v1/lab/{labId}/visits`

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/` | DESKROLE+ | Create visit (calls `VisitService.addVisit`) |
| GET | `/` | DESKROLE+ | List visits for lab |
| GET | `/{visitId}` | DESKROLE+ | Get specific visit with full detail |
| PUT | `/{visitId}/cancel` | ADMIN+ | Cancel visit |
| POST | `/{visitId}/complete` | ADMIN+ | Complete visit |
| GET | `/datewise` | TECHNICIAN+ | Date-wise visit listing |

---

### 6.3 `AdminStatsController` — `/api/v1/lab-admin/stats`

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/daily` | ADMIN+ | Today's KPIs from `daily_lab_stats` |
| GET | `/range` | ADMIN+ | KPI summary over a date range |
| GET | `/category` | ADMIN+ | Category-wise breakdown |
| GET | `/revenue/daily` | ADMIN+ | Daily revenue chart data |
| GET | `/revenue/by-collection-method` | ADMIN+ | Cash/UPI/Card breakdown |
| GET | `/billing/grid` | ADMIN+ | Paginated billing grid report |

---

### 6.4 `SuperAdminDashboardController` — `/api/v1/lab-super-admin`

| Method | Path | Role | Purpose |
|---|---|---|---|
| GET | `/stats/live` | SUPERADMIN | Real-time cross-lab KPIs |
| GET | `/stats/labs` | SUPERADMIN | Per-lab performance summary |
| GET | `/stats/revenue/top-labs` | SUPERADMIN | Top-8 labs by revenue |
| POST | `/stats/rollup/backfill` | SUPERADMIN | Trigger manual `daily_lab_stats` backfill |

---

## 7. Security Implementation

### 7.1 `SpringSecurityConfig` — Filter Chain Definition

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(headers -> headers
            .contentSecurityPolicy(csp ->
                csp.policyDirectives(
                    "default-src 'self'; script-src 'self'; " +
                    "object-src 'none'; frame-ancestors 'self'; base-uri 'self'"))
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)
                .includeSubDomains(true)
                .preload(true))
            .frameOptions(fo -> fo.sameOrigin()))
        .addFilterBefore(ipWhitelistFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(rateLimitFilter,   UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtFilter,         UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth
            // Public
            .requestMatchers("/auth/login", "/auth/register", "/auth/send-otp",
                             "/auth/verify-otp", "/auth/refresh", "/auth/forgot-password",
                             "/auth/reset-password", "/auth/validate-reset-token",
                             "/onboarding/**", "/public/labs").permitAll()
            // Role-scoped (abbreviated; full mapping in SecurityConfig source)
            .requestMatchers("/lab-super-admin/**").hasRole("SUPERADMIN")
            .requestMatchers("/user-management/**").hasAnyRole("SUPERADMIN", "ADMIN")
            .requestMatchers("/lab-admin/stats/**").hasAnyRole("SUPERADMIN", "ADMIN")
            .anyRequest().authenticated());

    return http.build();
}

@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();   // Spring Security default rounds (10)
}
```

---

### 7.2 CORS Configuration

Allowed origins, methods, and headers are configurable via environment variables. In production, origins are restricted to `*.tiameds.ai` subdomains. `allowCredentials(true)` is required for cross-origin cookie delivery with `SameSite=None`.

---

### 7.3 `MyUserDetails` — `UserDetails` Implementation

```java
public class MyUserDetails implements UserDetails {
    private final User user;

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
            .collect(Collectors.toSet());
    }
    @Override public String getPassword()  { return user.getPassword(); }
    @Override public String getUsername()  { return user.getUsername(); }
    @Override public boolean isEnabled()   { return user.isEnabled(); }
    // isAccountNonExpired, isAccountNonLocked, isCredentialsNonExpired → all return true
}
```

---

## 8. Utility Classes

### 8.1 `JwtUtil` — RS256 Token Generation

```java
public record JwtToken(String value, Instant expiresAt) {}
public record RefreshJwtToken(String value, Instant expiresAt, UUID id) {}

public class JwtUtil {

    // Generates access token (default TTL: 15 min)
    public JwtToken generateAccessToken(String username, Integer tokenVersion) {
        Instant now     = Instant.now();
        Instant expiry  = now.plus(jwtProperties.getAccessTokenTtl());

        String encryptedSubject = encryptionUtil.encryptSubject(username);

        String token = Jwts.builder()
            .setSubject(encryptedSubject)
            .setIssuer(jwtProperties.getIssuer())
            .setAudience(jwtProperties.getAudience())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiry))
            .claim("tokenVersion", tokenVersion)
            .claim("tokenType", "access")
            .signWith(privateKey, SignatureAlgorithm.RS256)
            .compact();

        return new JwtToken(token, expiry);
    }

    // Generates refresh token (default TTL: 24h); embeds UUID jti for DB revocation
    public RefreshJwtToken generateRefreshToken(String username, Integer tokenVersion) {
        UUID jti = UUID.randomUUID();
        // ... builds claims with jti + tokenType="refresh" + tokenVersion
        return new RefreshJwtToken(token, expiry, jti);
    }

    // Parses access token; throws ExpiredJwtException / SignatureException on failure
    public Claims parseAccessToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(publicKey)
            .requireIssuer(jwtProperties.getIssuer())
            .requireAudience(jwtProperties.getAudience())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public String extractUsername(String token) {
        String encryptedSubject = parseAccessToken(token).getSubject();
        return encryptionUtil.decryptSubject(encryptedSubject);
    }

    public Integer extractTokenVersion(String token) {
        return parseAccessToken(token).get("tokenVersion", Integer.class);
    }
}
```

**RSA key loading:**
- Private key: PKCS#8-encoded PEM → `PKCS8EncodedKeySpec` → `KeyFactory("RSA").generatePrivate()`
- Public key: X.509-encoded PEM → `X509EncodedKeySpec` → `KeyFactory("RSA").generatePublic()`
- Key file paths injected from `JwtProperties.privateKeyLocation` / `publicKeyLocation` (classpath resources in dev; Secrets Manager paths in production)

---

### 8.2 `EncryptionUtil` — AES/GCM Subject Encryption

```
Algorithm : AES/GCM/NoPadding
IV length  : 12 bytes  (GCM standard nonce)
Tag length : 128 bits  (GCM authentication tag)
Key sizes  : 16 / 24 / 32 bytes (AES-128 / AES-192 / AES-256)
Key source : Base64-decoded from `security.jwt.subject-encryption-key`

Wire format: Base64( IV[12 bytes] || CipherText )
Wrapper     : encrypt → "ENC(" + base64 + ")"
              decrypt → strips ENC(...) wrapper before decoding
```

```java
public String encrypt(String plainText) {
    byte[] iv = new byte[12];
    secureRandom.nextBytes(iv);

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
    byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

    byte[] output = new byte[iv.length + cipherText.length];
    System.arraycopy(iv, 0, output, 0, iv.length);
    System.arraycopy(cipherText, 0, output, iv.length, cipherText.length);

    return Base64.getEncoder().encodeToString(output);
}

public String encryptSubject(String subject) {
    return "ENC(" + encrypt(subject) + ")";
}
```

---

### 8.3 `SnowflakeIdentifierGenerator` — Hibernate Custom Generator

Used exclusively by `TestReferenceEntity`. Generates distributed-safe, time-sortable 64-bit `long` IDs.

```
Snowflake bit layout (64 bits total):
  1 bit  — sign (always 0)
 41 bits — milliseconds since custom epoch
 10 bits — worker/node ID
 12 bits — sequence within same millisecond

Custom epoch: a fixed timestamp (e.g. 2020-01-01T00:00:00Z)
Max IDs/ms/node: 4096
```

Registration in Hibernate via `@GenericGenerator(name="snowflake-id", strategy="...SnowflakeIdentifierGenerator")`.

---

### 8.4 `PasswordValidator`

Enforces the following complexity rules before BCrypt encoding:
- Minimum 8 characters
- At least one uppercase letter (`[A-Z]`)
- At least one lowercase letter (`[a-z]`)
- At least one digit (`[0-9]`)
- At least one special character (`[!@#$%^&*]`)

Throws `InvalidPasswordException` if any rule fails; exception message names the violated rule.

---

## 9. Configuration Beans

### 9.1 `JwtProperties` — `@ConfigurationProperties("security.jwt")`

```java
@ConfigurationProperties(prefix = "security.jwt")
@Validated
public class JwtProperties {
    @NotNull Resource privateKeyLocation;   // env: JWT_PRIVATE_KEY_LOCATION
    @NotNull Resource publicKeyLocation;    // env: JWT_PUBLIC_KEY_LOCATION
    Duration accessTokenTtl;               // default PT15M  (15 minutes)
    Duration refreshTokenTtl;              // default P1D    (24 hours)
    String   issuer;                       // default "tiameds-lab-automation"
    String   audience;                     // default "tiameds-clients"
    String   accessCookieName;             // default "accessToken"
    String   refreshCookieName;            // default "refreshToken"
    String   cookieDomain;                 // default ".tiameds.ai"
    String   cookiePath;                   // default "/"
    boolean  cookieSecure;                 // default true (false in dev)
    String   sameSite;                     // default "None" ("Lax" in dev)
    @NotBlank String subjectEncryptionKey; // AES key, Base64-encoded
}
```

---

### 9.2 `RollupAsyncConfig` — Thread Pool

```java
@Configuration @EnableAsync
public class RollupAsyncConfig {

    @Bean(name = "rollupTaskExecutor")
    public Executor rollupTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // always 2 alive threads
        executor.setMaxPoolSize(4);       // burst up to 4
        executor.setQueueCapacity(200);   // queue depth before rejection
        executor.setThreadNamePrefix("rollup-recompute-");
        executor.initialize();
        return executor;
    }
}
```

`RollupRecomputeListener` and `CategoryStatsRollupListener` are annotated `@Async("rollupTaskExecutor")` so they execute on this pool, not the request thread.

---

### 9.3 HikariCP — Connection Pool Settings

| Property | Value | Notes |
|---|---|---|
| `maximum-pool-size` | 40 | Total connections to RDS |
| `minimum-idle` | 0 | No idle connections kept — aggressive pool release |
| `max-lifetime` | 900,000 ms (15 min) | Prevents stale connection reuse |
| `auto-commit` | false | Explicit transaction management |
| `transaction-isolation` | READ_COMMITTED | PostgreSQL default |
| `cachePrepStmts` | true | Enable prepared statement cache |
| `prepStmtCacheSize` | 500 | Max cached statements |
| `prepStmtCacheSqlLimit` | 1024 chars | Max SQL length to cache |

---

### 9.4 Hibernate JPA Settings

| Property | Value |
|---|---|
| `hibernate.jdbc.batch_size` | 15 |
| `hibernate.order_inserts` | true |
| `hibernate.order_updates` | true |
| `hibernate.query.fail_on_pagination_over_collection_fetch` | true |
| `hibernate.query.in_clause_parameter_padding` | true |
| `hibernate.query.plan_cache_max_size` | 4096 |
| `hibernate.default_batch_fetch_size` | 10 |
| `hibernate.jdbc.time_zone` | UTC |
| `open-in-view` | false |
| `ddl-auto` | update (dev) / validate (prod recommended) |

---

## 10. Filter Chain — Implementation Detail

### 10.1 `JwtFilter` — Full Logic

```java
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PATHS = List.of(
        "/auth/login", "/auth/register", "/auth/send-otp", "/auth/verify-otp",
        "/auth/refresh", "/public/login", "/public/register"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {

        String path = req.getServletPath();
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            chain.doFilter(req, res); return;
        }

        // 1. Token extraction: cookie first, then Authorization header
        String token = null;
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            token = Arrays.stream(cookies)
                .filter(c -> jwtProperties.getAccessCookieName().equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);
        }
        if (token == null) {
            String header = req.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring(7);
            }
        }

        if (token == null) {
            sendUnauthorized(res, "No token provided"); return;
        }

        try {
            // 2. Parse and validate signature / expiry
            Claims claims = jwtUtil.parseAccessToken(token);

            // 3. Decrypt subject to get username
            String username = jwtUtil.extractUsername(token);

            // 4. Single DB call: load user (avoids separate UserDetailsService fetch)
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // 5. Validate tokenVersion claim vs DB value
            Integer claimVersion = claims.get("tokenVersion", Integer.class);
            if (!claimVersion.equals(user.getTokenVersion())) {
                sendUnauthorized(res, "Token has been revoked"); return;
            }

            // 6. Set SecurityContext
            MyUserDetails userDetails = new MyUserDetails(user);
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (ExpiredJwtException e) {
            sendUnauthorized(res, "Token expired");       return;
        } catch (JwtException e) {
            sendUnauthorized(res, "Invalid token");       return;
        }

        chain.doFilter(req, res);
    }

    private void sendUnauthorized(HttpServletResponse res, String message) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
```

---

### 10.2 `IpWhitelistFilter`

Enabled via `security.ip.whitelist.enabled=true`. When enabled, reads the comma-separated IP list from `security.ip.whitelist.ips`. Requests from non-listed IPs receive HTTP 403. Disabled by default — no-op in all current environments.

---

### 10.3 `RateLimitFilter`

Currently a **pass-through stub** — performs no actual rate limiting. The actual per-user rate limiting for login is done inside `AuthController` using Bucket4j in-memory token buckets. Password-reset rate limiting is done in `PasswordResetRateLimitService` using the `password_reset_rate_limits` database table.

---

## 11. Event-Driven Rollup — Class-Level Detail

### 11.1 Event Classes

```java
// Published after any write that changes billing revenue, visit count, or test results
public class RollupRecomputeEvent extends ApplicationEvent {
    private final Long labId;
    private final LocalDate date;

    public RollupRecomputeEvent(Object source, Long labId, LocalDate date) {
        super(source);
        this.labId = labId;
        this.date  = date;
    }
}

// Published after any write that changes per-category test counts or revenue
public class CategoryRollupEvent extends ApplicationEvent {
    private final Long labId;
    private final LocalDate date;
}
```

---

### 11.2 Listener Classes

```java
@Component
public class RollupRecomputeListener {

    @EventListener
    @Async("rollupTaskExecutor")           // runs on rollup-recompute-* thread
    @TransactionalEventListener(           // fires AFTER the publishing transaction commits
        phase = TransactionPhase.AFTER_COMMIT)
    public void onRollupRecompute(RollupRecomputeEvent event) {
        dashboardRollupService.recomputeDay(event.getLabId(), event.getDate());
    }
}

@Component
public class CategoryStatsRollupListener {

    @EventListener
    @Async("rollupTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryRollup(CategoryRollupEvent event) {
        categoryStatsRollupService.recomputeDay(event.getLabId(), event.getDate());
    }
}
```

**Key design note:** `@TransactionalEventListener(AFTER_COMMIT)` guarantees the rollup reads committed data. If the business transaction rolls back, the event is silently discarded and no rollup runs — correct behavior since there is nothing to re-aggregate.

---

### 11.3 Write Paths That Publish Events

| Service Method | Events Published |
|---|---|
| `VisitService.addVisit()` | `RollupRecomputeEvent`, `CategoryRollupEvent` |
| `BillingManagementService.createBilling()` | `RollupRecomputeEvent`, `CategoryRollupEvent` |
| `BillingManagementService.updateBilling()` | `RollupRecomputeEvent`, `CategoryRollupEvent` |
| `ReportService.saveReport()` | `RollupRecomputeEvent`, `CategoryRollupEvent` |
| `UpdatePatientService.cancelVisit()` | `RollupRecomputeEvent` |

---

## 12. Audit Subsystem — Implementation Detail

### 12.1 `@Auditable` Annotation

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String module();      // e.g. "PATIENT", "BILLING", "VISIT"
    String action();      // "CREATE", "UPDATE", "DELETE", "VIEW"
    SeverityLevel severity() default SeverityLevel.LOW;
}
```

---

### 12.2 `AuditAspect` — `@Around` Advice

```
Pointcut : @annotation(com.tiameds.audit.Auditable)
Advice   : @Around

BEFORE proceed:
    - Extract entity DTO from method arguments (type inspection)
    - For UPDATE actions: snapshot existing record from DB as `oldValueSnapshot`

CALL pjp.proceed()  ← actual controller method executes

AFTER proceed (in finally block):
    - Authentication user = SecurityContextHolder.getContext().getAuthentication()
    - Extract role = first role of authenticated user
    - Extract ipAddress from X-Forwarded-For header (first IP) or request.getRemoteAddr()
    - Extract deviceInfo from User-Agent header
    - Extract requestId from X-Request-ID header
    - Extract labId, patientId, visitId, billingId, testId from URI path variables
      via HttpServletRequest.getAttribute("org.springframework.web.servlet.HandlerMapping.uriTemplateVariables")

    - For CREATE:
        newValue = serialize(responseBody or requestDTO)
        oldValue = null

    - For UPDATE:
        oldValue = serialize(oldValueSnapshot)
        newValue = serialize(updatedEntity from DB)
        fieldChanged = FieldChangeTracker.computeDiff(oldValueSnapshot, updatedEntity)

    - Build LabAuditLogs entity
    - AuditLogService.persistAsync(auditLog)  ← non-blocking
```

---

### 12.3 `FieldChangeTracker`

```java
public class FieldChangeTracker {

    // Uses reflection to compare all fields of two objects of the same type.
    // Returns Map<fieldName, [oldValue, newValue]> for fields that differ.
    public Map<String, Object[]> computeDiff(Object before, Object after) {
        Map<String, Object[]> changes = new LinkedHashMap<>();
        for (Field field : before.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object oldVal = field.get(before);
            Object newVal = field.get(after);
            if (!Objects.equals(oldVal, newVal)) {
                changes.put(field.getName(), new Object[]{oldVal, newVal});
            }
        }
        return changes;
    }
}
```

---

### 12.4 `AuditLogService`

```java
@Service
public class AuditLogService {

    @Async                           // Uses default async executor (not rollupTaskExecutor)
    public void persistAsync(LabAuditLogs auditLog) {
        try {
            auditLogsRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to persist audit log: {}", e.getMessage(), e);
            // Swallowed — audit failure never affects the business operation
        }
    }
}
```

---

## 13. Flyway Migration Catalogue

All scripts live in `src/main/resources/db/migration/`. Flyway baseline is **V10** — migrations V1–V10 were applied manually before Flyway adoption.

| Version | Script | Change |
|---|---|---|
| V11 | `V11__dashboard_indexes.sql` | Indexes supporting dashboard aggregate queries |
| V12 | `V12__fix_billing_column_types.sql` | Column type corrections on `billing` table |
| V13 | `V13__fix_billing_transaction_column_types.sql` | Numeric/timestamp fixes on `billing_transaction` |
| V14 | `V14__fix_tests_price_column_type.sql` | `price` column → NUMERIC |
| V15 | `V15__fix_timestamp_column_types.sql` | TIMESTAMP WITH TIME ZONE alignment |
| V16 | `V16__fix_refresh_tokens_id_column_type.sql` | UUID type on `refresh_tokens.id` |
| **V17** | `V17__create_daily_lab_stats.sql` | **Creates `daily_lab_stats` with composite PK (lab_id, stat_date)** |
| V18 | `V18__fix_user_email_correction.sql` | User email field corrections |
| V19 | `V19__fix_password_reset_sequences.sql` | Sequence fixes for password reset |
| V20 | `V20__fix_missing_id_sequence_defaults.sql` | Missing SERIAL/IDENTITY defaults |
| V21 | `V21__fix_billing_actual_received_amount_precision.sql` | NUMERIC(19,4) precision fix |
| V22 | `V22__fix_billing_timestamp_column_types.sql` | TIMESTAMPTZ on billing timestamps |
| V23 | `V23__create_ai_clinical_observations.sql` | Creates `ai_clinical_observations` table |
| V24 | `V24__fix_billing_time_column_type.sql` | `billing_time` → TIME type |
| V25 | `V25__fix_remaining_billing_numeric_columns.sql` | Remaining numeric precision fixes |
| V26 | `V26__fix_remaining_billing_transaction_numeric_columns.sql` | Transaction amount precision |
| V27 | `V27__fix_test_discount_numeric_columns.sql` | Discount amount precision |
| V28 | `V28__fix_billing_transaction_created_at_column_type.sql` | TIMESTAMPTZ fix |
| V29 | `V29__fix_remaining_timestamp_column_drift.sql` | Remaining TIMESTAMP → TIMESTAMPTZ |
| V30 | `V30__add_health_packages_is_active_column.sql` | Adds `is_active BOOLEAN DEFAULT TRUE` |
| V31 | `V31__fix_lab_audit_logs_column_types.sql` | JSONB/TEXT/TIMESTAMPTZ on audit logs |
| V32 | `V32__fix_lab_report_jsonb_column_types.sql` | `reportJson`, `testRows`, `referenceRanges` → JSONB |
| V33 | `V33__fix_expires_at_and_used_at_column_types.sql` | Token expiry column types |
| V34 | `V34__fix_remaining_instant_column_types.sql` | Java Instant → TIMESTAMPTZ alignment |
| V35 | `V35__fix_localdate_column_types.sql` | Java LocalDate → DATE alignment |
| V36 | `V36__fix_super_admin_test_price_column_type.sql` | Price precision on super admin test |
| V37 | `V37__fix_remaining_jsonb_column_types.sql` | Remaining String-mapped JSONB columns |
| V38 | `V38__fix_visit_test_result_timestamp_columns.sql` | TIMESTAMPTZ on VisitTestResult |
| V39 | `V39__add_lab_report_visit_lab_index.sql` | `CREATE INDEX ON lab_report(visit_id, lab_id)` |
| V40 | `V40__restore_missing_primary_keys_and_identity.sql` | Restores broken PK/IDENTITY sequences |
| **V41** | `V41__create_daily_lab_category_stats.sql` | **Creates `daily_lab_category_stats` with composite PK (lab_id, stat_date, category)** |

---

## 14. Application Properties — Full Reference

### 14.1 `application.yml` (base — all profiles)

```yaml
server:
  servlet:
    context-path: /api/v1
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/javascript,text/css
  error:
    whitelabel:
      enabled: false          # Custom JSON error format; no Spring error page

spring:
  profiles:
    active: dev               # Overridden by SPRING_PROFILES_ACTIVE env var in ECS

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

  datasource:
    url: jdbc:postgresql://${DB_URL}:5432/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      auto-commit: false
      minimum-idle: 0
      maximum-pool-size: 40
      max-lifetime: 900000
      transaction-isolation: TRANSACTION_READ_COMMITTED
      cachePrepStmts: true
      prepStmtCacheSize: 500
      prepStmtCacheSqlLimit: 1024
      useServerPrepStmts: false

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 10
    validate-on-migrate: true   # Strict — fails on schema/checksum mismatch

  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: update          # Applied in dev; should be 'validate' in prod
    properties:
      hibernate:
        jdbc:
          batch_size: 15
          time_zone: UTC
        order_inserts: true
        order_updates: true
        query:
          fail_on_pagination_over_collection_fetch: true
          in_clause_parameter_padding: true
          plan_cache_max_size: 4096
        default_batch_fetch_size: 10

  mail:
    host: ${SPRING_MAIL_HOST:smtp.gmail.com}
    port: ${SPRING_MAIL_PORT:587}
    username: ${SPRING_MAIL_USERNAME:itadmin@tiameds.ai}
    password: ${SPRING_MAIL_PASSWORD}
    properties:
      mail.smtp.auth: ${SPRING_MAIL_SMTP_AUTH:true}
      mail.smtp.starttls.enable: ${SPRING_MAIL_SMTP_STARTTLS_ENABLE:true}

logging:
  level:
    org.springframework.security: WARN
    com.fasterxml.jackson: WARN
    tiameds: INFO

security:
  jwt:
    issuer:              ${JWT_ISSUER:tiameds-lab-automation}
    audience:            ${JWT_AUDIENCE:tiameds-clients}
    access-token-ttl:    ${JWT_ACCESS_TOKEN_TTL:PT15M}
    refresh-token-ttl:   ${JWT_REFRESH_TOKEN_TTL:P1D}
    private-key-location: ${JWT_PRIVATE_KEY_LOCATION}
    public-key-location:  ${JWT_PUBLIC_KEY_LOCATION}
    access-cookie-name:  ${JWT_ACCESS_COOKIE_NAME:accessToken}
    refresh-cookie-name: ${JWT_REFRESH_COOKIE_NAME:refreshToken}
    cookie-domain:       ${JWT_COOKIE_DOMAIN:.tiameds.ai}
    cookie-path:         ${JWT_COOKIE_PATH:/}
    cookie-secure:       ${JWT_COOKIE_SECURE:true}
    same-site:           ${JWT_COOKIE_SAMESITE:None}
    subject-encryption-key: ${JWT_SUBJECT_ENCRYPTION_KEY}
  ip:
    whitelist:
      enabled: false
      ips: ""

rate:
  limit:
    login:
      attempts: 5         # Bucket4j token bucket capacity
      window: 10          # minutes — refill period

aws:
  s3:
    bucket:            ${AWS_S3_BUCKET}
    region:            ${AWS_REGION}
    cdn-base-url:      ${AWS_S3_CDN_BASE_URL}
    presign-expiry-minutes: ${AWS_S3_PRESIGN_EXPIRY_MINUTES:10}

onboarding:
  token:
    expiry-minutes: ${ONBOARDING_TOKEN_EXPIRY_MINUTES:15}
  rate-limit:
    max-emails-per-hour: ${ONBOARDING_RATE_MAX_EMAILS_PER_HOUR:3}
    window-minutes:      ${ONBOARDING_RATE_WINDOW_MINUTES:60}
  frontend:
    base-url:           ${ONBOARDING_FRONTEND_BASE_URL:https://lab-prod.tiameds.ai}
    verification-url:   ${ONBOARDING_VERIFICATION_URL:https://lab-prod.tiameds.ai/verify-email}
    onboarding-url:     ${ONBOARDING_ONBOARDING_URL:https://lab-prod.tiameds.ai/onboarding}

password:
  reset:
    token:
      expiry:
        minutes: ${PASSWORD_RESET_TOKEN_EXPIRY_MINUTES:15}
      length: ${PASSWORD_RESET_TOKEN_LENGTH:32}     # bytes → 64 hex chars in email
    url: ${PASSWORD_RESET_URL:https://lab-prod.tiameds.ai/reset-password}
    rate:
      limit:
        email:
          max: ${PASSWORD_RESET_RATE_LIMIT_EMAIL_MAX:3}
          window:
            minutes: ${PASSWORD_RESET_RATE_LIMIT_EMAIL_WINDOW:1}
        ip:
          max: ${PASSWORD_RESET_RATE_LIMIT_IP_MAX:3}
          window:
            minutes: ${PASSWORD_RESET_RATE_LIMIT_IP_WINDOW:1}
```

---

### 14.2 `application-dev.yml` (local development overrides)

```yaml
server:
  error:
    include-message: always
    include-stacktrace: on-param
    include-binding-errors: always

logging:
  level:
    org.hibernate.SQL: DEBUG       # Prints every SQL query
    tiameds: DEBUG                 # Full application debug logging

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/localdb?charSet=UTF8
    username: postgres
    password: root
  jpa:
    hibernate:
      ddl-auto: validate           # No auto-schema in dev — Flyway only

security:
  jwt:
    private-key-location: ${JWT_PRIVATE_KEY_LOCATION:classpath:keys/private_key.pem}
    public-key-location:  ${JWT_PUBLIC_KEY_LOCATION:classpath:keys/public_key.pem}
    cookie-secure: ${JWT_COOKIE_SECURE:false}   # HTTP-safe cookies for localhost
    same-site:     ${JWT_COOKIE_SAMESITE:Lax}   # Lax for same-origin dev

onboarding:
  frontend:
    base-url:         http://localhost:3000
    verification-url: http://localhost:3000/verify-email
    onboarding-url:   http://localhost:3000/onboarding

password:
  reset:
    url: http://localhost:3000/reset-password
```

---

## 15. Sequence Diagrams — Key Flows

### 15.1 Login Flow (Detailed)

```
Client                  AuthController          UserService / JwtUtil           DB
  │                          │                         │                        │
  │── POST /auth/login ─────►│                         │                        │
  │   {username, password}   │                         │                        │
  │                          │── findByUsername() ────────────────────────────►│
  │                          │◄── User entity ────────────────────────────────│
  │                          │                         │                        │
  │                          │  BCryptPasswordEncoder.matches(password, hash)  │
  │                          │  [FAILS → 401 Unauthorized]                     │
  │                          │  [PASSES → continue]                            │
  │                          │                         │                        │
  │                          │── checkRateLimit(username) ──►Bucket4j          │
  │                          │   [EXCEEDED → 429 Too Many Requests]            │
  │                          │                                                 │
  │                          │── jwtUtil.generateAccessToken(username, tokenVersion)
  │                          │   └── AES/GCM encrypt username → encryptedSubject
  │                          │   └── RS256 sign JWT (15 min TTL)               │
  │                          │                                                 │
  │                          │── jwtUtil.generateRefreshToken(username, tokenVersion)
  │                          │   └── RS256 sign JWT (24h TTL) + UUID jti       │
  │                          │── refreshTokenRepository.save(RefreshToken) ──►│
  │                          │                                                 │
  │◄── 200 OK ──────────────│                                                 │
  │   AuthResponse body      │                                                 │
  │   Set-Cookie: accessToken=...;  HttpOnly; Secure; SameSite=None; Domain=.tiameds.ai
  │   Set-Cookie: refreshToken=...; HttpOnly; Secure; SameSite=None; Domain=.tiameds.ai
```

---

### 15.2 Create Visit Flow (Detailed)

```
Client              VisitController         VisitService                     DB           EventBus
  │                      │                      │                            │               │
  │── POST /lab/{labId}/visits ────────────────►│                            │               │
  │   VisitDTO body       │                      │                            │               │
  │                       │── @Auditable         │                            │               │
  │                       │   AuditAspect fires  │                            │               │
  │                       │                      │                            │               │
  │                       │  [1] Validate lab exists and user is member ─────►│               │
  │                       │  [2] Validate patient belongs to lab ────────────►│               │
  │                       │  [3] Fetch Test, HealthPackage, Doctor,           │               │
  │                       │      Insurance entities by ID ───────────────────►│               │
  │                       │  [4] Generate visitCode: "VS-XXXXX"               │               │
  │                       │      LabEntitySequence PESSIMISTIC_WRITE lock ───►│               │
  │                       │  [5] Create VisitEntity with all relationships    │               │
  │                       │  [6] Create BillingEntity from BillingDTO         │               │
  │                       │      Generate billingCode: "BL-XXXXX" ──────────►│               │
  │                       │  [7] visit.billing = billing (cascade)            │               │
  │                       │  [8] visitRepository.save(visit) ────────────────►│               │
  │                       │  [9] Publish RollupRecomputeEvent ──────────────────────────────►│
  │                       │  [10] Publish CategoryRollupEvent ──────────────────────────────►│
  │◄── 201 Created ──────│                      │                            │               │
  │   VisitDTO response   │                      │   [AFTER_COMMIT]           │               │
  │                       │                      │   ◄──── RollupRecomputeListener fires ───│
  │                       │                      │   DashboardRollupService.recomputeDay()  │
  │                       │                      │   → upsert daily_lab_stats ─────────────►│
  │                       │                      │   ◄──── CategoryStatsRollupListener ────│
  │                       │                      │   CategoryStatsRollupService.recomputeDay()
  │                       │                      │   → delete + upsert daily_lab_category_stats
```

---

### 15.3 Token Refresh Flow

```
Client                  AuthController          JwtUtil           DB
  │                          │                    │               │
  │── POST /auth/refresh ───►│                    │               │
  │   Cookie: refreshToken   │                    │               │
  │                          │  Extract refresh cookie            │
  │                          │── jwtUtil.parseRefreshToken(token) │
  │                          │   Verify RS256 signature + expiry  │
  │                          │   Extract jti (UUID)               │
  │                          │── refreshTokenRepository.findByJti() ────────►│
  │                          │◄── RefreshToken record ───────────────────────│
  │                          │   [not found or tokenType != refresh → 401]   │
  │                          │                                               │
  │                          │── refreshTokenRepository.delete(old) ────────►│
  │                          │── generateAccessToken(username, tokenVersion)  │
  │                          │── generateRefreshToken(username, tokenVersion) │
  │                          │── refreshTokenRepository.save(new) ──────────►│
  │◄── 200 OK ──────────────│                                               │
  │   New Set-Cookie headers │                                               │
```

---

### 15.4 Audit Write Flow (Asynchronous)

```
Request Thread                    AuditAspect                AuditLogService        DB
      │                               │                            │                │
      │  @Auditable method called     │                            │                │
      ├──────────────────────────────►│                            │                │
      │                               │  snapshot oldValue         │                │
      │                               │  pjp.proceed()             │                │
      │◄── response returned ─────────│                            │                │
      │                               │                            │                │
      │                               │  build LabAuditLogs        │                │
      │                               │  computeDiff(old, new)     │                │
      │                               │─── persistAsync(log) ─────►│                │
      │                               │   [returns immediately]     │  @Async thread │
      │◄── HTTP response sent ────────│                            │─────────────►│ │
      │   (audit write is off-path)   │                            │  save(log)    │ │
```

---

## 16. ID Generation Strategies — Implementation

### Summary Table

| Strategy | Entities | Mechanism | Format |
|---|---|---|---|
| `GenerationType.IDENTITY` | User, Lab, Patient, Visit, Test, Billing, BillingTransaction, Sample, VisitSample, VisitTestResult, Doctors, HealthPackage, Insurance, Role, Module, OTP, PasswordResetToken, RefreshToken, VerificationToken, ReportEntity, ReportSettings, AiClinicalObservation, SuperAdminTest, SuperAdminReference | PostgreSQL SERIAL / IDENTITY | Numeric long |
| UUID `@GeneratedValue` | `LabAuditLogs` | `GenerationType.AUTO` → Hibernate UUID generator | UUID string |
| Custom Snowflake | `TestReferenceEntity` | `SnowflakeIdentifierGenerator` registered via `@GenericGenerator` | 64-bit long, time-sortable |
| Sequence-based codes | All entities with `*Code` fields | `SequenceGeneratorService` + `LabEntitySequence` table (per-lab, per-type counter) | "PREFIX-NNNNN" |

### Code Prefix Reference

| Entity Type | Column | Prefix | Example |
|---|---|---|---|
| Patient | `patient_code` | `PT` | `PT-00042` |
| Visit | `visit_code` | `VS` | `VS-00117` |
| Billing | `billing_code` | `BL` | `BL-00089` |
| Sample | `sample_code` | `SA` | `SA-00003` |
| VisitSample | `visit_sample_code` | `VSP` | `VSP-00055` |
| VisitTestResult | `visit_test_result_code` | `VTR` | `VTR-00201` |
| Test | `test_code` | `TST` | `TST-00015` |
| Test Reference | `test_reference_code` | `TR` | `TR-00012` |
| Doctor | `doctor_code` | `DR` | `DR-00007` |
| Health Package | `package_code` | `PKG` | `PKG-00002` |
| User | `user_code` | `USR` | `USR-00001` |
| Report | `report_code` | `RPT` | `RPT-00300` |

---

## 17. JSON / JSONB Handling

### 17.1 Hibernate `@JdbcTypeCode(JSON)` Mapping

Fields annotated with `@JdbcTypeCode(SqlTypes.JSON)` (imported as `@JdbcTypeCode(JSON)`) are stored as PostgreSQL `jsonb` columns. Hibernate 6 serializes/deserializes them via Jackson `ObjectMapper`.

| Entity | Field | Java Type | JSONB Content |
|---|---|---|---|
| `ReportEntity` | `reportJson` | `String` | Full rendered report payload |
| `ReportEntity` | `referenceRanges` | `String` | Dynamic reference ranges |
| `ReportEntity` | `testRows` | `List<TestRow>` | Multi-row test results; Jackson deserializes to `TestRow` objects |
| `TestReferenceEntity` | `reportJson` | `String` | Report structure template |
| `TestReferenceEntity` | `referenceRanges` | `String` | Age/gender-specific ranges |
| `TestReferenceEntity` | `dropdown` | `String` | UI dropdown definitions |
| `TestReferenceEntity` | `impression` | `String` | Clinical impression template |
| `LabAuditLogs` | `oldValue` | `String` | Before-state entity snapshot |
| `LabAuditLogs` | `newValue` | `String` | After-state entity snapshot |
| `LabAuditLogs` | `fieldChanged` | `String` | Field-by-field diff map |

### 17.2 `TestRow` — JSONB List Element

```java
public class TestRow {
    String testName;
    String enteredValue;
    String unit;
    String referenceRange;
    String referenceAgeRange;
    String description;
    String remarks;
}
```

---

## 18. Cross-Cutting Design Decisions

### 18.1 No Cascade on Lab ↔ Patient

```java
// PatientEntity:
@ManyToMany(fetch = FetchType.LAZY)   // ← NO cascade attribute
@JoinTable(name = "lab_patients", ...)
private Set<Lab> labs;
```

**Why:** If `CascadeType.ALL` were set, saving a new `Patient` would cascade a persist operation to the `Lab`, which would in turn cascade to its many other relationships, potentially triggering `TransientPropertyValueException` or creating duplicate records. The association is managed explicitly by the service layer.

---

### 18.2 `open-in-view: false` — Strict Transaction Boundaries

Setting `spring.jpa.open-in-view=false` means the JPA session is closed when the service method returns. Any lazy-loaded collection access after that point throws `LazyInitializationException`. This forces developers to fetch all needed data within the service transaction — preventing the "N+1 in the view layer" anti-pattern. DTOs must be fully populated before the transaction closes.

---

### 18.3 Rollup Idempotency — Full Re-aggregation vs. Increment

The rollup services **never increment**; they always re-aggregate the entire day from source tables. This means:
- Calling `recomputeDay` 100 times for the same day is safe.
- Any data correction (billing update, cancellation) automatically self-corrects on next rollup.
- There is no risk of double-counting from re-delivered events.

The cost is that each rollup call runs 6 aggregate queries on source tables. This is acceptable given the `rollupTaskExecutor` runs it asynchronously on a dedicated 2–4 thread pool, not the request thread.

---

### 18.4 Token Revocation via `tokenVersion`

The `tokenVersion` field in the `users` table enables server-side session invalidation without maintaining a token blocklist:

```
User.tokenVersion = 3
JWT claims: { tokenVersion: 3, ... }

On password change or admin-forced logout:
  UPDATE users SET token_version = token_version + 1 WHERE id = ?
  → token_version becomes 4

Next request with old token (tokenVersion=3):
  JwtFilter: claimVersion(3) ≠ user.tokenVersion(4) → 401 Unauthorized
```

This requires one DB read per request (to fetch User and compare `tokenVersion`), which the `JwtFilter` already performs for the `UserDetails` load — there is no extra cost.

---

### 18.5 DB-Backed Rate Limiting for Password Reset (Cross-Instance Safety)

Login rate limiting uses Bucket4j **in-memory** token buckets. This works for a single instance but would be bypassed under multiple ECS replicas (each instance has its own bucket).

Password reset rate limiting uses a **database table** (`password_reset_rate_limits`) to track counts per email and per IP:

```
Client Request → PasswordResetRateLimitService
    └── SELECT count, window_start FROM password_reset_rate_limits
        WHERE identifier = :email AND window_start > NOW() - INTERVAL '1 minute'
    [count >= 3] → throw TooManyRequestsException → 429
    [count < 3]  → INSERT or UPDATE count + 1 → proceed
```

This is safe across any number of replicas because all instances read/write the same RDS table. A unique constraint on `(identifier_type, identifier_value, window_start)` prevents race conditions under concurrent requests.

---

### 18.6 `@TransactionalEventListener(AFTER_COMMIT)` — Rollup Timing

If the rollup listener used `@EventListener` (without `@TransactionalEventListener`), it could execute **before the business transaction commits**. In that case, the re-aggregation query would read the pre-commit state and produce stale numbers.

With `TransactionPhase.AFTER_COMMIT`, Spring delays event delivery until after the transaction successfully commits. If the transaction rolls back, the event is silently discarded.

---

### 18.7 `fail_on_pagination_over_collection_fetch=true`

When a JPQL query uses `LIMIT`/`OFFSET` (pagination) on a root entity that has a `@OneToMany` or `@ManyToMany` collection fetched in the same query, Hibernate must load **all rows into memory** and paginate in-application rather than in SQL — a silent correctness problem that can exhaust heap.

Setting `fail_on_pagination_over_collection_fetch=true` makes Hibernate throw an exception rather than silently doing in-memory pagination. This forces developers to separate collection fetches from paginated queries (use `@EntityGraph` on a separate query, or batch-fetch the collections after pagination).

---

*This document was reverse-engineered entirely from the production codebase of the `tiameds` Spring Boot application and reflects the implementation as of September 2026. All class names, method signatures, SQL statements, and configuration values are sourced directly from the actual source files — no aspirational content is included.*
