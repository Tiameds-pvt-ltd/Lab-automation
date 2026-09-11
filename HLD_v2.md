# TiaMeds Lab Automation System — HLD v2
> Spring Boot 3 · Java 17 · Multi-tenant · AWS ECS Fargate · PostgreSQL · September 2026

---

## 1 · System Context

```
┌──────────────────────────────────────────────────────────────────────┐
│                          CLIENTS                                     │
│        Web App / Mobile App / Admin Dashboard / HMIS Partner         │
└───────────────────────────────┬──────────────────────────────────────┘
                                │ HTTPS  (JWT in HttpOnly Cookie)
┌───────────────────────────────▼──────────────────────────────────────┐
│                  TiaMeds Lab API  (Spring Boot 3)                    │
│  ┌────────────┐  ┌──────────────┐  ┌───────────────────────────┐    │
│  │ Auth &     │  │  Core Lab    │  │  Analytics / Super-Admin  │    │
│  │ Security   │  │  Operations  │  │  Dashboard Rollups        │    │
│  └────────────┘  └──────────────┘  └───────────────────────────┘    │
│  Filter chain: Rate Limit → IP Whitelist → JWT Auth → Controller     │
│  AOP: AuditAspect (entity changes)  │  Async: RollupThreadPool      │
└────────┬──────────────┬─────────────┴──────────────┬────────────────┘
         │              │                             │
  ┌──────▼──────┐ ┌─────▼──────┐             ┌───────▼──────┐
  │ PostgreSQL  │ │  AWS S3    │             │  Gmail SMTP  │
  │ (RDS/HikariCP│ │Logos/Signs │             │  OTP / Reset │
  │  max 40 conn)│ │presigned  │             │  Verify Mail │
  └─────────────┘ └────────────┘             └──────────────┘
```

| Layer | Choice |
|---|---|
| Runtime | Java 17 · Spring Boot 3.3.4 · Embedded Tomcat |
| Security | Spring Security 6 · RS256 JWT · Bucket4j rate-limit |
| Persistence | Hibernate 6 · HikariCP · Flyway · PostgreSQL JSONB |
| Cloud | AWS ECS Fargate · ECR · RDS · S3 · Secrets Manager · CloudWatch |

---

## 2 · Domain Model — Entity Relationship Diagram

```
  ┌──────────┐  M:M users_roles   ┌──────────┐
  │   ROLE   │◄──────────────────►│   USER   │
  │ SUPERADMIN│                   │──────────│
  │ ADMIN    │  M:M users_modules │ username │
  │ TECH     │◄──────────────────►│ email    │
  │ DESKROLE │   ┌────────────┐   │ tokenVer │ ← increment = revoke all sessions
  └──────────┘   │  MODULE    │   └────┬─────┘
                 │ (feature   │        │ M:M  lab_members
                 │  flags)    │        ▼
                 └────────────┘  ┌───────────────────────────────┐
                                 │              LAB               │
                                 │───────────────────────────────│
                                 │ lab_id · name · region        │
                                 │ licenseNumber · isActive       │
                                 └──────┬──────────────┬──────────┘
                          ┌────────────┤              │
                    1:N   │            │ 1:N          │ 1:N
              ┌───────────▼──┐  ┌──────▼──────┐  ┌───▼──────────────┐
              │  DAILY_STATS  │  │   PATIENT   │  │      TEST         │
              │  (per lab+day)│  │─────────────│  │──────────────────│
              │ totalVisits  │  │ patient_id  │  │ test_id · code   │
              │ paidRevenue  │  │ name · dob  │  │ name · category  │
              │ dueRevenue   │  │ mobile      │  │ price · refRange  │
              │ patientCount │  │ guardianId──┼─►│ (lab_tests join) │
              │ testCount    │  │ (self-ref)  │  └────────┬──────────┘
              └──────────────┘  └──────┬──────┘           │ M:M
                                       │ 1:N              │
                              ┌────────▼──────────────────▼────────────┐
                              │                 VISIT                   │
                              │────────────────────────────────────────│
                              │ visit_id · visitCode · visitType        │
                              │ visitStatus · hmisOrderRef (HMIS link)  │
                              └────┬──────────┬──────────┬─────────────┘
                                   │ 1:1      │ 1:N      │ M:M via visit_samples
                            ┌──────▼───┐  ┌───▼──────────▼─────────────┐
                            │ BILLING  │  │     VISIT_TEST_RESULT        │
                            │──────────│  │────────────────────────────│
                            │billing_id│  │ result_id · value · flag   │
                            │totalAmt  │  │ reportStatus               │
                            │cgst,sgst │  │ PENDING / COMPLETED /      │
                            │netAmount │  │ CANCELLED                  │
                            │dueAmount │  └────────────────────────────┘
                            └────┬─────┘
                                 │ 1:N       Also linked to VISIT:
                          ┌──────▼──────┐   ┌─────────────────────────┐
                          │TRANSACTION  │   │ REPORT  (1:1)           │
                          │─────────────│   │  JSONB: testRows,       │
                          │ amount      │   │  referenceRanges,       │
                          │ CASH/UPI    │   │  reportJson             │
                          │ CARD        │   ├─────────────────────────┤
                          └─────────────┘   │ SAMPLE  (M:M via        │
                                            │  visit_samples)         │
                                            ├─────────────────────────┤
                                            │ AI_CLINICAL_OBS  (1:1)  │
                                            ├─────────────────────────┤
                                            │ INSURANCE  (M:M)        │
                                            ├─────────────────────────┤
                                            │ HEALTH_PACKAGE  (M:M)   │
                                            └─────────────────────────┘
```

**Supporting entities (auth / audit / onboarding):**

```
  USER ──1:N──► REFRESH_TOKEN       VISIT ──M:M──► DOCTOR
  USER ──1:N──► OTP                  LAB  ──1:1──► REPORT_SETTINGS
  USER ──1:N──► PASSWORD_RESET_TOKEN LAB  ──1:1──► LAB_ENTITY_SEQUENCE (code counters)
  *ALL entity mutations ──► LAB_AUDIT_LOGS (JSONB: oldValue, newValue, fieldChanged)
```

---

## 3 · Module Breakdown & Visit Lifecycle

| Module | Key Flows |
|---|---|
| **Auth** | Register → OTP/Password login → RS256 JWT (15 min) + Refresh (24h) → Revoke via tokenVersion |
| **Patient** | Create patient → assign patientCode → link to lab → search / update |
| **Visit** | Open visit → attach tests & packages → collect samples → enter results → finalise |
| **Report** | Auto-generate PDF on finalise → store JSONB (testRows, refRanges) → S3 presigned URL |
| **Billing** | Invoice on close → GST split (CGST/SGST/IGST) → multi-method payments (CASH/UPI/CARD) |
| **Analytics** | Writes trigger RollupRecomputeEvent → async DashboardRollupService → pre-agg daily_lab_stats |
| **Super-Admin** | Cross-lab dashboard (reads daily_lab_stats only) · Lab onboarding · Master test data |

**Patient Visit Lifecycle (sequential flow):**

```
  [1] Register Patient → [2] Open Visit → [3] Attach Tests/Packages
       ↓
  [4] Collect Samples → [5] Enter Test Results → [6] Finalise Report
       ↓
  [7] Close Billing (GST calc + payment) → [8] Complete Visit
       ↓
  [→] Triggers Rollup → daily_lab_stats updated async (non-blocking)
  [→] Triggers AuditAspect → lab_audit_logs written async
```

---

## 4 · HMIS Integration Architecture

```
┌───────────────────────────────────────────────────────────────────┐
│                         HMIS Platform                             │
│                                                                   │
│  ┌──────────┐  ┌──────────┐  ┌────────────┐  ┌──────────────┐   │
│  │   OPD    │  │   IPD    │  │  Pharmacy  │  │  Radiology   │   │
│  │  Doctor  │  │  Wards   │  │            │  │    (RIS)     │   │
│  └────┬─────┘  └──────────┘  └────────────┘  └──────────────┘   │
│       │  Lab Order (REST / HL7 FHIR ServiceRequest)              │
│  ┌────▼──────────────────────────────────────────────────────┐   │
│  │          HMIS Integration Bus  (REST or Kafka)            │   │
│  └────┬──────────────────────────────────────────────────────┘   │
└───────┼───────────────────────────────────────────────────────────┘
        │  POST /api/v1/visits  { hmisOrderId, hmisPatientId, tests[] }
┌───────▼───────────────────────────────────────────────────────────┐
│                  TiaMeds Lab API                                   │
│  ① Upsert Patient (stores hmisPatientId as externalRef)           │
│  ② Open Visit    (stores hmisOrderRef on VisitEntity)             │
│  ③ Attach Tests  → VisitTestResult rows                           │
│  ④ Enter Results → Report PDF → S3                               │
│  ⑤ POST hmisCallbackUrl { results[], reportUrl, billingAmount }   │
└───────────────────────────────────────────────────────────────────┘
        │  Callback pushed to HMIS bus
┌───────▼───────────────────────────────────────────────────────────┐
│  HMIS routes: result → EMR/Doctor · charges → Central Billing     │
│               notification → Patient                              │
└───────────────────────────────────────────────────────────────────┘
```

**Integration patterns at a glance:**

| Flow | Protocol | Direction | Notes |
|---|---|---|---|
| Lab order creation | REST / HL7 FHIR | HMIS → Lab | `hmisOrderId` stored on Visit |
| Patient demographics | Webhook / Kafka | HMIS → Lab | Lab caches locally for offline use |
| Result + report ready | REST callback | Lab → HMIS | Presigned S3 URL for PDF |
| Lab charges | REST / Kafka event | Lab → HMIS Billing | Lab billing is authoritative |
| Daily analytics | REST pull / Kafka | Lab → HMIS Dashboard | Anonymised daily_lab_stats only |

> **Design principle:** Lab operates standalone if the HMIS bus is down. Walk-in orders work independently; HMIS sync retries on reconnect.

---

## 5 · Multi-Region Deployment + NFRs

```
                  ┌────────────────────────────────┐
                  │       Global Control Plane      │
                  │  Route 53 Geo-DNS · CloudFront  │
                  │  Super-Admin (aggregated view)  │
                  └────────┬──────────┬─────────────┘
                           │          │
           ┌───────────────┤          ├──────────────────┐
           │               │          │                  │
  ┌────────▼──────┐ ┌──────▼───────┐ ┌▼────────────────┐│
  │  Region: IND  │ │ Region: MEA  │ │ Region: SEA     ││
  │  (Mumbai)     │ │ (UAE/Riyadh) │ │ (Singapore)     ││
  │               │ │              │ │                 ││
  │ Lab API       │ │ Lab API      │ │ Lab API         ││
  │ RDS pg ←DPDP │ │ RDS pg ←PDPL │ │ RDS pg ←PDPA   ││
  │ S3 ap-south-1 │ │ S3 me-central│ │ S3 ap-southeast ││
  │ SES email     │ │ SES email    │ │ SES email       ││
  └───────┬───────┘ └──────┬───────┘ └┬────────────────┘│
          │                │           │                 │
          └────────────────┴───────────┘                 │
                           │  Anonymised daily_lab_stats only (no PII)
                    ┌──────▼──────────────┐
                    │ Central Analytics DB │
                    │ (Super-Admin rollup) │
                    └─────────────────────┘
```

**Tenant routing:** `region` is set at lab onboarding (immutable). Client apps resolve base URL at login:

```
api-ind.tiameds.ai  →  India cluster
api-mea.tiameds.ai  →  Middle East cluster
api-sea.tiameds.ai  →  SE Asia cluster
```

**Key Non-Functional Properties:**

| Property | How |
|---|---|
| Multi-tenancy | All queries scoped to `labId`; no cross-lab data leakage |
| Stateless API | No server-side session; JWT carries all auth context |
| Idempotent rollups | Full recompute per day — safe to re-run; startup backfill on crash |
| Audit trail | AOP-based; async write; before/after JSONB + field diff |
| Data residency | Per-region RDS; only anonymised stats cross regions |
| Rate limiting | Login: Bucket4j (5/10min) · Password reset: DB-backed (cross-replica safe) |
| Secrets | 40+ vars from AWS Secrets Manager; zero plaintext in image or Git |

**Rollout Phases:**

| Phase | Scope |
|---|---|
| 1 · Now | Single-region IND — go-live with first hospital network |
| 2 | HMIS REST callback integration + shared patient contract |
| 3 | MEA region deployment (full stack replica, data residency) |
| 4 | SEA region + Central Analytics DB + Federated Super-Admin view |
| 5 | Kafka event bus (replace REST callbacks when orders > 500/day) |

---

*TiaMeds Lab Automation System · HLD v2 · September 2026*
