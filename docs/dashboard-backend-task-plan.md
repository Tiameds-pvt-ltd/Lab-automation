# Dashboard — Backend Only Task & Sub-Task Plan

**Project:** Tiameds Lab Automation
**Feature:** Analytics Dashboard — Backend APIs
**Date:** 2026-06-29
**Tech Stack:** Spring Boot, JPA/Hibernate, PostgreSQL

---

## Access Rules

| Role | Dashboard Scope | Base Path |
|------|----------------|-----------|
| **Super Admin** | All labs — aggregate | `/api/v1/super-admin/dashboard/` |
| **Lab Admin** | Their lab only — `labId` scoped | `/api/v1/lab/{labId}/dashboard/` |

---

## What Already Exists (Do NOT rebuild)

| Repository | Existing Queries |
|-----------|-----------------|
| `BillingRepository` | `sumTotalByLabId`, `countByLabId`, `sumDiscountByLabId` — with date range |
| `VisitRepository` | `countByLabIdAndCreatedAtBetween`, `countByLabIdAndStatus` |
| `PatientRepository` | `countByLabIdAndCreatedAtBetween` |
| `TransactionRepository` | `findTransactionsByLabAndDateRange` |
| `DoctorRepository` | `countByLabId` |
| `HealthPackageRepository` | `countByLabId` |

**Everything below is NET NEW work.**

---

## Table of Contents

1. [Task 1 — New DB Table: lab_alerts](#task-1--new-db-table-lab_alerts)
2. [Task 2 — New Repository Queries](#task-2--new-repository-queries)
3. [Task 3 — DTOs](#task-3--dtos)
4. [Task 4 — Super Admin Dashboard Service](#task-4--super-admin-dashboard-service)
5. [Task 5 — Super Admin Dashboard Controller](#task-5--super-admin-dashboard-controller)
6. [Task 6 — Lab Admin Dashboard Service](#task-6--lab-admin-dashboard-service)
7. [Task 7 — Lab Admin Dashboard Controller](#task-7--lab-admin-dashboard-controller)
8. [Task 8 — Alerts Engine](#task-8--alerts-engine)
9. [Task 9 — Security & Auth Guards](#task-9--security--auth-guards)
10. [Hour Summary](#task-10--hour-summary)

---

## Task 1 — New DB Table: lab_alerts

**File:** `entity/LabAlert.java` + `repository/LabAlertRepository.java`

### Why
No alerts system exists. Dashboard shows "42 reports delayed", "23 samples pending > 4 hrs" etc. — need a table to store these.

### Sub-Tasks

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 1.1 | Create `LabAlert` entity | Fields: `id`, `labId` (nullable — null = system-wide for super admin), `alertType` (enum), `message`, `count`, `severity` (CRITICAL/WARNING/INFO), `isResolved`, `createdAt`, `resolvedAt` | 2 hrs |
| 1.2 | Create `AlertType` enum | Values: `REPORT_DELAYED`, `SAMPLE_PENDING`, `CRITICAL_REPORT`, `LOW_SAMPLE_COLLECTION` | 0.5 hr |
| 1.3 | Create `AlertSeverity` enum | Values: `CRITICAL`, `WARNING`, `INFO` | 0.5 hr |
| 1.4 | Create `LabAlertRepository` | Methods: `findByLabIdAndIsResolvedFalse`, `findByLabIdIsNullAndIsResolvedFalse` (super admin), `resolveAlert` | 1 hr |
| **TOTAL** | | | **4 hrs** |

---

## Task 2 — New Repository Queries

Add JPQL/native queries to existing repositories. No new repositories needed except `LabAlertRepository`.

### 2A — BillingRepository (add new queries)

| # | Method | JPQL / Purpose | Hours |
|---|--------|---------------|-------|
| 2A.1 | `sumTotalAcrossAllLabs(from, to)` | `SELECT SUM(b.totalAmount) FROM BillingEntity b WHERE b.createdAt BETWEEN :from AND :to` — super admin total revenue | 1 hr |
| 2A.2 | `sumRevenuePerLabBetween(from, to)` | Returns `List<Object[]>` with `[labId, labName, SUM(totalAmount)]` grouped by lab — for "Revenue by Lab" chart | 1.5 hrs |
| 2A.3 | `dailyRevenueTrendAllLabs(from, to)` | Native query: `SELECT DATE(created_at), SUM(total_amount) FROM billing WHERE created_at BETWEEN ? AND ? GROUP BY DATE(created_at) ORDER BY DATE(created_at)` | 1.5 hrs |
| 2A.4 | `dailyRevenueTrendByLab(labId, from, to)` | Same as above but scoped to one lab via billing→visit→patient→labs join | 1.5 hrs |
| 2A.5 | `sumRevenueByPaymentMethod(labId, from, to)` | `SELECT t.paymentMethod, SUM(t.receivedAmount) FROM TransactionEntity t JOIN t.billing b JOIN b.labs l WHERE l.id = :labId ... GROUP BY t.paymentMethod` | 1.5 hrs |
| **TOTAL** | | | **7 hrs** |

### 2B — VisitRepository (add new queries)

| # | Method | JPQL / Purpose | Hours |
|---|--------|---------------|-------|
| 2B.1 | `countDistinctPatientsAllLabs(from, to)` | Count distinct patient IDs across all labs in period — super admin KPI | 1 hr |
| 2B.2 | `countPendingSamplesAllLabs(from, to)` | Count visits where `visitStatus IN ('PENDING','COLLECTED','IN_PROGRESS')` across all labs | 1 hr |
| 2B.3 | `countVisitsByStatusAllLabs(from, to)` | Returns `List<Object[]>` with `[visitStatus, COUNT]` — for "Sample Status Overview" donut | 1 hr |
| 2B.4 | `countVisitsByStatusForLab(labId, from, to)` | Same but scoped to one lab | 1 hr |
| 2B.5 | `countPendingOlderThan(thresholdInstant)` | Count visits where `visitStatus = 'PENDING'` AND `createdAt < :threshold` — alert detection | 1 hr |
| 2B.6 | `countPendingOlderThanForLab(labId, thresholdInstant)` | Same scoped to lab | 1 hr |
| 2B.7 | `sampleFunnelForLab(labId, from, to)` | Returns 5 counts: registered (all visits), collected, results entered, reports generated, reports delivered — using `visitStatus` stages | 2 hrs |
| **TOTAL** | | | **8 hrs** |

### 2C — ReportRepository (add new queries)

| # | Method | JPQL / Purpose | Hours |
|---|--------|---------------|-------|
| 2C.1 | `countAllReportsAllLabs(from, to)` | `SELECT COUNT(r) FROM ReportEntity r WHERE r.createdAt BETWEEN :from AND :to` | 0.5 hr |
| 2C.2 | `countReportsByLab(labId, from, to)` | Count reports for specific lab in period | 0.5 hr |
| 2C.3 | `countReportsByCategory(from, to)` | `SELECT r.testCategory, COUNT(r) FROM ReportEntity r ... GROUP BY r.testCategory` — all labs | 1 hr |
| 2C.4 | `countReportsByCategoryForLab(labId, from, to)` | Same but scoped to one lab | 1 hr |
| 2C.5 | `topOrderedTestsForLab(labId, from, to, limit)` | `SELECT r.testName, COUNT(r) FROM ReportEntity r WHERE r.labId = :labId ... GROUP BY r.testName ORDER BY COUNT(r) DESC` | 1 hr |
| 2C.6 | `countDelayedReports(thresholdHours)` | Native query: reports where `(created_at - visit.created_at) > N hours` AND `report_status != 'DELIVERED'` — alert detection | 2 hrs |
| 2C.7 | `countDelayedReportsByLab(labId, thresholdHours)` | Same scoped to lab | 1 hr |
| **TOTAL** | | | **7 hrs** |

### 2D — UserRepository (add new queries)

| # | Method | JPQL / Purpose | Hours |
|---|--------|---------------|-------|
| 2D.1 | `countAllByRole(roleName)` | `SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName` — super admin user type counts | 1 hr |
| 2D.2 | `countByLabIdAndRole(labId, roleName)` | Count users in specific lab by role (ADMIN, DESK_USER, TECHNICIAN) | 1 hr |
| 2D.3 | `findByLabIdAndRole(labId, roleName)` | Returns `List<User>` — for technician performance table | 1 hr |
| **TOTAL** | | | **3 hrs** |

### 2E — VisitTestResultRepository (add new queries)

| # | Method | JPQL / Purpose | Hours |
|---|--------|---------------|-------|
| 2E.1 | `countAllTestsAllLabs(from, to)` | `SELECT COUNT(vtr) FROM VisitTestResult vtr JOIN vtr.visit v JOIN v.patient p JOIN p.labs l WHERE vtr.createdAt BETWEEN :from AND :to` | 1 hr |
| 2E.2 | `countTestsByLab(labId, from, to)` | Scoped to one lab | 1 hr |
| 2E.3 | `technicianPerformanceForLab(labId, from, to)` | `SELECT vtr.createdBy, COUNT(vtr) FROM VisitTestResult vtr ... GROUP BY vtr.createdBy` — for technician table | 2 hrs |
| **TOTAL** | | | **4 hrs** |

### 2F — DoctorRepository (add new queries)

| # | Method | JPQL / Purpose | Hours |
|---|--------|---------------|-------|
| 2F.1 | `topReferringDoctorsForLab(labId, from, to, limit)` | `SELECT d.id, d.name, COUNT(v), SUM(b.totalAmount) FROM Doctors d JOIN d.labs l JOIN ... visits v ... billing b WHERE l.id = :labId GROUP BY d.id ORDER BY COUNT(v) DESC` | 2 hrs |
| 2F.2 | `topReferringDoctorsAllLabs(from, to, limit)` | Same across all labs, includes lab count per doctor | 2 hrs |
| **TOTAL** | | | **4 hrs** |

### 2G — PatientRepository (add new queries)

| # | Method | JPQL / Purpose | Hours |
|---|--------|---------------|-------|
| 2G.1 | `genderDistributionForLab(labId, from, to)` | `SELECT p.gender, COUNT(p) FROM PatientEntity p JOIN p.labs l JOIN p.visits v WHERE l.id = :labId AND v.createdAt BETWEEN :from AND :to GROUP BY p.gender` | 1.5 hrs |
| 2G.2 | `ageGroupDistributionForLab(labId, from, to)` | Native query using `DATE_PART('year', AGE(date_of_birth))` to bucket into 0-18, 19-35, 36-50, 51-65, 65+ | 2.5 hrs |
| 2G.3 | `countDistinctPatientsByLab(labId, from, to)` | Count distinct patients with visits in period for this lab | 1 hr |
| **TOTAL** | | | **5 hrs** |

### 2H — HealthPackageRepository (add new queries)

| # | Method | JPQL / Purpose | Hours |
|---|--------|---------------|-------|
| 2H.1 | `packagePerformanceForLab(labId, from, to)` | Join `patient_visit_packages` → `health_packages` → `billing`, return: `[packageId, packageName, COUNT(bookings), SUM(revenue)]` | 2.5 hrs |
| **TOTAL** | | | **2.5 hrs** |

---

## Task 3 — DTOs

New response DTOs for dashboard endpoints. Place in `dto/dashboard/`.

| # | DTO Class | Fields | Hours |
|---|-----------|--------|-------|
| 3.1 | `SuperAdminKpiSummaryDTO` | `totalLabs`, `totalAdmins`, `totalDeskUsers`, `totalTechnicians`, `totalTests`, `totalRevenue`, `pendingSamples`, `reportsGenerated` — each with `currentValue` + `trendPercent` + `trendDirection` | 1.5 hrs |
| 3.2 | `LabAdminKpiSummaryDTO` | `totalRevenue`, `totalTests`, `totalPatients`, `pendingSamples`, `reportsGenerated`, `avgTatHours`, `activeAdmins`, `deskUsers`, `technicians` — each with trend fields | 1.5 hrs |
| 3.3 | `RevenueTrendDTO` | `List<DailyRevenuePoint>` where each point has `date` (String) + `revenue` (BigDecimal) | 0.5 hr |
| 3.4 | `RevenueByLabDTO` | `labId`, `labName`, `revenue` — list for bar chart | 0.5 hr |
| 3.5 | `CategoryBreakdownDTO` | `category`, `count`, `percentage` — reusable for Tests by Category + Sample Status | 0.5 hr |
| 3.6 | `LabPerformanceSummaryDTO` | `rank`, `labId`, `labName`, `revenue`, `tests`, `patients`, `pendingSamples`, `avgTatHours`, `reportsGenerated`, `growthPercent` | 1 hr |
| 3.7 | `TopDoctorDTO` | `doctorId`, `doctorName`, `labsCount`, `patientsCount`, `revenue` | 0.5 hr |
| 3.8 | `AlertDTO` | `alertId`, `alertType`, `message`, `count`, `severity`, `labId`, `labName`, `createdAt` | 0.5 hr |
| 3.9 | `SampleFunnelDTO` | `registered`, `collected`, `resultsEntered`, `reportsGenerated`, `reportsDelivered` — each with `count` + `percentage` | 0.5 hr |
| 3.10 | `PaymentMethodBreakdownDTO` | `method` (UPI/Cash/Card/Credit), `amount`, `percentage` | 0.5 hr |
| 3.11 | `TechnicianPerformanceDTO` | `userId`, `technicianName`, `samplesProcessed`, `reportsEntered`, `avgTatHours` | 0.5 hr |
| 3.12 | `PackagePerformanceDTO` | `packageId`, `packageName`, `bookings`, `revenue` | 0.5 hr |
| 3.13 | `AgeGenderDistributionDTO` | `genderBreakdown` (List of `CategoryBreakdownDTO`) + `ageGroupBreakdown` (List of `CategoryBreakdownDTO`) | 0.5 hr |
| **TOTAL** | | | **8.5 hrs** |

---

## Task 4 — Super Admin Dashboard Service

**File:** `services/dashboard/SuperAdminDashboardService.java`

All methods accept `LocalDate from, LocalDate to`. Internally convert to `Instant` using UTC midnight.

| # | Method | What It Does | Hours |
|---|--------|-------------|-------|
| 4.1 | `getKpiSummary(from, to)` | Calls: `countAllLabs`, `countUsersByRole(ADMIN)`, `countUsersByRole(DESK_USER)`, `countUsersByRole(TECHNICIAN)`, `countAllTests`, `sumRevenueAllLabs`, `countPendingSamplesAllLabs`, `countAllReports`. For each metric computes trend % by fetching same-length prior period. Returns `SuperAdminKpiSummaryDTO` | 4 hrs |
| 4.2 | `getRevenueTrend(from, to)` | Calls `dailyRevenueTrendAllLabs`, maps to `List<DailyRevenuePoint>` | 1.5 hrs |
| 4.3 | `getRevenueByLab(from, to, limit)` | Calls `sumRevenuePerLabBetween`, sorts descending, takes top N, maps to `List<RevenueByLabDTO>` | 1.5 hrs |
| 4.4 | `getTestsByCategory(from, to)` | Calls `countReportsByCategory`, computes % per category, maps to `List<CategoryBreakdownDTO>` | 1.5 hrs |
| 4.5 | `getSampleStatusOverview(from, to)` | Calls `countVisitsByStatusAllLabs`, maps statuses to display labels (PENDING→Pending, COLLECTED→Collected, etc.), computes %, returns `List<CategoryBreakdownDTO>` | 1.5 hrs |
| 4.6 | `getLabPerformanceSummary(from, to, page, size)` | For each lab: revenue, tests, patients, pending samples, avg TAT, reports, growth %. Growth = compare current vs prior period. TAT = native SQL AVG of `(lab_report.created_at - patient_visits.created_at)` in hours per lab | 5 hrs |
| 4.7 | `getTopReferringDoctors(from, to, limit)` | Calls `topReferringDoctorsAllLabs`, maps to `List<TopDoctorDTO>` | 1.5 hrs |
| 4.8 | `getAlerts()` | Calls `LabAlertRepository.findByLabIdIsNullAndIsResolvedFalse()`, maps to `List<AlertDTO>` | 1 hr |
| **TOTAL** | | | **18 hrs** |

---

## Task 5 — Super Admin Dashboard Controller

**File:** `controller/superAdmin/SuperAdminDashboardController.java`
**Security:** `hasRole('SUPER_ADMIN')` on all endpoints

| # | Endpoint | Method | Params | Response DTO | Hours |
|---|----------|--------|--------|-------------|-------|
| 5.1 | `/super-admin/dashboard/kpi-summary` | GET | `from` (date), `to` (date) | `SuperAdminKpiSummaryDTO` | 1 hr |
| 5.2 | `/super-admin/dashboard/revenue-trend` | GET | `from`, `to` | `List<DailyRevenuePoint>` | 0.5 hr |
| 5.3 | `/super-admin/dashboard/revenue-by-lab` | GET | `from`, `to`, `limit` (default 8) | `List<RevenueByLabDTO>` | 0.5 hr |
| 5.4 | `/super-admin/dashboard/tests-by-category` | GET | `from`, `to` | `List<CategoryBreakdownDTO>` | 0.5 hr |
| 5.5 | `/super-admin/dashboard/sample-status-overview` | GET | `from`, `to` | `List<CategoryBreakdownDTO>` | 0.5 hr |
| 5.6 | `/super-admin/dashboard/lab-performance-summary` | GET | `from`, `to`, `page`, `size` | `Page<LabPerformanceSummaryDTO>` | 0.5 hr |
| 5.7 | `/super-admin/dashboard/top-referring-doctors` | GET | `from`, `to`, `limit` (default 5) | `List<TopDoctorDTO>` | 0.5 hr |
| 5.8 | `/super-admin/dashboard/alerts` | GET | — | `List<AlertDTO>` | 0.5 hr |
| **TOTAL** | | | | | **4.5 hrs** |

---

## Task 6 — Lab Admin Dashboard Service

**File:** `services/dashboard/LabAdminDashboardService.java`

All methods take `Long labId, LocalDate from, LocalDate to`. Internally validate that the calling user is a member of this lab (reuse existing `userRepository.existsByIdAndLabsId`).

| # | Method | What It Does | Hours |
|---|--------|-------------|-------|
| 6.1 | `getKpiSummary(labId, from, to)` | Calls: `sumTotalByLabId` (revenue), `countTestsByLab`, `countDistinctPatientsByLab`, `countPendingSamplesForLab`, `countReportsByLab`, avg TAT query, `countByLabIdAndRole(ADMIN)`, `countByLabIdAndRole(DESK_USER)`, `countByLabIdAndRole(TECHNICIAN)`. Computes trend % vs prior period. Returns `LabAdminKpiSummaryDTO` | 4 hrs |
| 6.2 | `getRevenueTrend(labId, from, to)` | Calls `dailyRevenueTrendByLab`, maps to `List<DailyRevenuePoint>` | 1.5 hrs |
| 6.3 | `getSampleFunnel(labId, from, to)` | Calls `sampleFunnelForLab` — returns 5 steps with count + % conversion each | 2 hrs |
| 6.4 | `getTestsByCategory(labId, from, to)` | Calls `countReportsByCategoryForLab`, computes % | 1 hr |
| 6.5 | `getTopOrderedTests(labId, from, to, limit)` | Calls `topOrderedTestsForLab`, returns `List<Object[]>` mapped to test name + count | 1 hr |
| 6.6 | `getRevenueByPaymentMethod(labId, from, to)` | Calls `sumRevenueByPaymentMethod`, maps to `List<PaymentMethodBreakdownDTO>` with % | 1.5 hrs |
| 6.7 | `getTechnicianPerformance(labId, from, to)` | Calls `technicianPerformanceForLab` (groups by `createdBy`), resolves username → `User` to get full name, computes per-technician avg TAT | 3 hrs |
| 6.8 | `getTopReferringDoctors(labId, from, to, limit)` | Calls `topReferringDoctorsForLab` | 1 hr |
| 6.9 | `getPackagePerformance(labId, from, to)` | Calls `packagePerformanceForLab` | 1 hr |
| 6.10 | `getAgeGenderDistribution(labId, from, to)` | Calls `genderDistributionForLab` + `ageGroupDistributionForLab`, assembles `AgeGenderDistributionDTO` | 2 hrs |
| 6.11 | `getAlerts(labId)` | Calls `LabAlertRepository.findByLabIdAndIsResolvedFalse(labId)` | 0.5 hr |
| **TOTAL** | | | **18.5 hrs** |

---

## Task 7 — Lab Admin Dashboard Controller

**File:** `controller/lab/LabDashboardController.java`
**Security:** User must be member of `labId` (reuse `requireLabMember` pattern from `ReportSettingController`)

| # | Endpoint | Method | Params | Response DTO | Hours |
|---|----------|--------|--------|-------------|-------|
| 7.1 | `/lab/{labId}/dashboard/kpi-summary` | GET | `from`, `to` | `LabAdminKpiSummaryDTO` | 1 hr |
| 7.2 | `/lab/{labId}/dashboard/revenue-trend` | GET | `from`, `to` | `List<DailyRevenuePoint>` | 0.5 hr |
| 7.3 | `/lab/{labId}/dashboard/sample-funnel` | GET | `from`, `to` | `SampleFunnelDTO` | 0.5 hr |
| 7.4 | `/lab/{labId}/dashboard/tests-by-category` | GET | `from`, `to` | `List<CategoryBreakdownDTO>` | 0.5 hr |
| 7.5 | `/lab/{labId}/dashboard/top-ordered-tests` | GET | `from`, `to`, `limit` (default 5) | `List<TestOrderCountDTO>` | 0.5 hr |
| 7.6 | `/lab/{labId}/dashboard/revenue-by-payment-method` | GET | `from`, `to` | `List<PaymentMethodBreakdownDTO>` | 0.5 hr |
| 7.7 | `/lab/{labId}/dashboard/technician-performance` | GET | `from`, `to` | `List<TechnicianPerformanceDTO>` | 0.5 hr |
| 7.8 | `/lab/{labId}/dashboard/top-referring-doctors` | GET | `from`, `to`, `limit` (default 5) | `List<TopDoctorDTO>` | 0.5 hr |
| 7.9 | `/lab/{labId}/dashboard/package-performance` | GET | `from`, `to` | `List<PackagePerformanceDTO>` | 0.5 hr |
| 7.10 | `/lab/{labId}/dashboard/age-gender-distribution` | GET | `from`, `to` | `AgeGenderDistributionDTO` | 0.5 hr |
| 7.11 | `/lab/{labId}/dashboard/alerts` | GET | — | `List<AlertDTO>` | 0.5 hr |
| **TOTAL** | | | | | **5.5 hrs** |

---

## Task 8 — Alerts Engine

**File:** `services/dashboard/AlertDetectionService.java`
Runs on a schedule, detects issues, inserts into `lab_alerts` table.

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 8.1 | Alert detection job | `@Scheduled(fixedDelay = 1800000)` — runs every 30 min | 0.5 hr |
| 8.2 | Detect: Reports delayed | Query `countDelayedReportsByLab` for all labs where delay > 4 hrs. If count > 0, upsert alert with `alertType = REPORT_DELAYED`, `count = N`, `severity = CRITICAL` | 2 hrs |
| 8.3 | Detect: Samples pending > 4 hrs | Query `countPendingOlderThanForLab` for all labs. Upsert alert with `alertType = SAMPLE_PENDING`, `severity = WARNING` | 2 hrs |
| 8.4 | Detect: Critical reports awaiting review | Query reports where `reportStatus = 'PENDING_REVIEW'`. Upsert `CRITICAL_REPORT` alert with `severity = CRITICAL` | 1.5 hrs |
| 8.5 | Detect: Low sample collection (super admin) | Compare each lab's current week sample count vs 4-week rolling average. If < 70%, upsert `LOW_SAMPLE_COLLECTION` alert with `severity = WARNING` | 2 hrs |
| 8.6 | Upsert logic | Before inserting, check if unresolved alert of same type + lab already exists. If yes — update `count` and `updatedAt`. Avoids duplicate alerts | 1.5 hrs |
| 8.7 | Resolve alert API | `PUT /lab/{labId}/alerts/{alertId}/resolve` — sets `isResolved = true`, `resolvedAt = now()` | 1 hr |
| 8.8 | Resolve alert API — super admin | `PUT /super-admin/alerts/{alertId}/resolve` | 0.5 hr |
| **TOTAL** | | | **11 hrs** |

---

## Task 9 — Security & Auth Guards

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 9.1 | Super Admin role check | Annotate `SuperAdminDashboardController` with `@PreAuthorize("hasRole('SUPER_ADMIN')")` — already have role infrastructure | 0.5 hr |
| 9.2 | Lab membership check | In `LabDashboardController`, reuse the `requireLabMember(labId)` pattern — user must belong to the given labId | 0.5 hr |
| 9.3 | Date param validation | Validate `from <= to`, both required, max range = 365 days. Return `400` if invalid | 1 hr |
| 9.4 | Default date range | If `from`/`to` not supplied, default to last 7 days | 0.5 hr |
| **TOTAL** | | | **2.5 hrs** |

---

## Task 10 — Hour Summary

### By Task

| Task | Area | Hours |
|------|------|-------|
| 1 | New DB table + entity (lab_alerts) | 4 hrs |
| 2A | BillingRepository — new queries | 7 hrs |
| 2B | VisitRepository — new queries | 8 hrs |
| 2C | ReportRepository — new queries | 7 hrs |
| 2D | UserRepository — new queries | 3 hrs |
| 2E | VisitTestResultRepository — new queries | 4 hrs |
| 2F | DoctorRepository — new queries | 4 hrs |
| 2G | PatientRepository — new queries | 5 hrs |
| 2H | HealthPackageRepository — new queries | 2.5 hrs |
| 3 | DTOs (13 classes) | 8.5 hrs |
| 4 | Super Admin Dashboard Service | 18 hrs |
| 5 | Super Admin Dashboard Controller (8 APIs) | 4.5 hrs |
| 6 | Lab Admin Dashboard Service | 18.5 hrs |
| 7 | Lab Admin Dashboard Controller (11 APIs) | 5.5 hrs |
| 8 | Alerts Engine + Scheduled Job | 11 hrs |
| 9 | Security & Auth Guards | 2.5 hrs |
| **GRAND TOTAL** | | **~113 hrs** |

---

### By Developer Type

| Developer | Tasks | Hours |
|-----------|-------|-------|
| Backend Developer | Tasks 1–9 (everything above) | 113 hrs |

---

### Delivery Estimate

| Sprints | Work | Duration |
|---------|------|----------|
| Sprint 1 | New entity + all repository queries (Tasks 1–2) | 1 week |
| Sprint 2 | DTOs + Super Admin Service + Controller (Tasks 3–5) | 1 week |
| Sprint 3 | Lab Admin Service + Controller (Tasks 6–7) | 1 week |
| Sprint 4 | Alerts engine + security + testing | 1 week |
| **Total** | | **~4 weeks (1 backend dev)** |

---

## API Reference Summary

### Super Admin APIs — 8 Endpoints

```
GET  /api/v1/super-admin/dashboard/kpi-summary              ?from=&to=
GET  /api/v1/super-admin/dashboard/revenue-trend            ?from=&to=
GET  /api/v1/super-admin/dashboard/revenue-by-lab           ?from=&to=&limit=8
GET  /api/v1/super-admin/dashboard/tests-by-category        ?from=&to=
GET  /api/v1/super-admin/dashboard/sample-status-overview   ?from=&to=
GET  /api/v1/super-admin/dashboard/lab-performance-summary  ?from=&to=&page=0&size=10
GET  /api/v1/super-admin/dashboard/top-referring-doctors    ?from=&to=&limit=5
GET  /api/v1/super-admin/dashboard/alerts
PUT  /api/v1/super-admin/alerts/{alertId}/resolve
```

### Lab Admin APIs — 11 Endpoints

```
GET  /api/v1/lab/{labId}/dashboard/kpi-summary              ?from=&to=
GET  /api/v1/lab/{labId}/dashboard/revenue-trend            ?from=&to=
GET  /api/v1/lab/{labId}/dashboard/sample-funnel            ?from=&to=
GET  /api/v1/lab/{labId}/dashboard/tests-by-category        ?from=&to=
GET  /api/v1/lab/{labId}/dashboard/top-ordered-tests        ?from=&to=&limit=5
GET  /api/v1/lab/{labId}/dashboard/revenue-by-payment-method ?from=&to=
GET  /api/v1/lab/{labId}/dashboard/technician-performance   ?from=&to=
GET  /api/v1/lab/{labId}/dashboard/top-referring-doctors    ?from=&to=&limit=5
GET  /api/v1/lab/{labId}/dashboard/package-performance      ?from=&to=
GET  /api/v1/lab/{labId}/dashboard/age-gender-distribution  ?from=&to=
GET  /api/v1/lab/{labId}/dashboard/alerts
PUT  /api/v1/lab/{labId}/alerts/{alertId}/resolve
```

**Total APIs: 21**
