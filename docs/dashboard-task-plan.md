# Dashboard — Detailed Task & Sub-Task Plan

**Project:** Tiameds Lab Automation
**Feature:** Analytics Dashboard
**Date:** 2026-06-29
**Status:** Not Started

---

## Dashboard Access Rules

| Role | Dashboard | Scope |
|------|-----------|-------|
| **Super Admin** | Super Admin Dashboard | All labs — aggregate view across entire network |
| **Lab Admin** | Lab Admin Dashboard | Only their assigned lab — drill-down view |

---

## Table of Contents

1. [Architecture & Setup](#1-architecture--setup)
2. [Shared Components](#2-shared-components)
3. [Super Admin Dashboard — All Labs Overview](#3-super-admin-dashboard--all-labs-overview)
4. [Lab Admin Dashboard — Single Lab View](#4-lab-admin-dashboard--single-lab-view)
5. [Backend APIs — Super Admin](#5-backend-apis--super-admin)
6. [Backend APIs — Lab Admin](#6-backend-apis--lab-admin)
7. [Filters & Date Range Logic](#7-filters--date-range-logic)
8. [Alerts & Insights Engine](#8-alerts--insights-engine)
9. [Download Report Feature](#9-download-report-feature)
10. [Testing](#10-testing)
11. [Hour Summary Table](#11-hour-summary-table)

---

## 1. Architecture & Setup

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 1.1 | Frontend project setup | Install charting library (Recharts / Chart.js / ApexCharts), date picker, table libs | 2 hrs |
| 1.2 | Role-based routing | Route guard — redirect Super Admin to `/super-admin/dashboard`, Lab Admin to `/lab/{labId}/dashboard` | 2 hrs |
| 1.3 | Dashboard layout shell | Sidebar nav, top header bar (logo, date filter, bell, avatar), main content area | 4 hrs |
| 1.4 | Sidebar navigation | Labs, Lab Admins, Desk Users, Technicians, Activity Logs, Master Data sections, Settings, Support, Active Labs count chip | 3 hrs |
| 1.5 | Global date range picker | From/To date selector, presets: Today, This Week, This Month, Custom | 3 hrs |
| 1.6 | Notification bell | Badge count, dropdown list of recent alerts, mark-all-read action | 3 hrs |
| 1.7 | API service layer | Axios/Fetch wrapper, auth header injection, error handling, loading states | 3 hrs |
| **TOTAL** | | | **20 hrs** |

---

## 2. Shared Components

Components reused across both dashboards.

| # | Component | Details | Hours |
|---|-----------|---------|-------|
| 2.1 | KPI Stat Card | Icon + value + label + trend badge (↑↓ % vs last period), green/red color logic | 4 hrs |
| 2.2 | Line Chart | Configurable axes, tooltips, time-series X-axis, responsive | 4 hrs |
| 2.3 | Donut / Pie Chart | Legend with color dots + labels + %, center total label | 3 hrs |
| 2.4 | Horizontal Bar Chart | Sorted bars, value labels on right, color scheme | 3 hrs |
| 2.5 | Data Table | Sortable columns, pagination, action column with icon buttons, row hover, colored badge for status | 4 hrs |
| 2.6 | Funnel Chart | Step-by-step funnel with count + % conversion per step | 4 hrs |
| 2.7 | Alert / Insight Card | Icon (error/warning/info), message text, "View" link, severity color coding | 2 hrs |
| 2.8 | Time period selector | "This Week / This Month" dropdown toggle on chart header | 1 hr |
| 2.9 | "View All" pagination modal | Reusable modal/drawer to show full list for doctors, tests, packages | 3 hrs |
| 2.10 | Trend badge | Small chip showing "+22.5% vs last week" — positive green, negative red | 1 hr |
| **TOTAL** | | | **29 hrs** |

---

## 3. Super Admin Dashboard — All Labs Overview

**Access:** Super Admin only
**Route:** `/super-admin/dashboard`
**Header tag:** "Level 1: All Labs Overview"

### 3A. Header Section

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 3A.1 | Page title & subtitle | "Super Admin Dashboard", "Level 1: All Labs Overview" badge, subtitle text | 1 hr |
| 3A.2 | Date range in header | Show selected range "15 May 2024 – 22 May 2024", open date picker on click | 1 hr |
| 3A.3 | Filters button | Opens filter panel — filter by city, lab type, active/inactive | 2 hrs |

### 3B. KPI Summary Cards (Top Row — 8 cards)

| # | Card | Data Source | Hours |
|---|------|-------------|-------|
| 3B.1 | Total Labs | Count of all labs in system | 1 hr |
| 3B.2 | Total Admins | Count of users with ADMIN role across all labs | 1 hr |
| 3B.3 | Total Desk Users | Count of users with DESK_USER role | 1 hr |
| 3B.4 | Total Technicians | Count of users with TECHNICIAN role | 1 hr |
| 3B.5 | Total Tests | Sum of all test-results / visit-tests in period | 1 hr |
| 3B.6 | Total Revenue | Sum of `billing.net_amount` across all labs in period | 1 hr |
| 3B.7 | Pending Samples | Count of visits with status = PENDING / IN_PROGRESS | 1 hr |
| 3B.8 | Reports Generated | Count of `lab_report` records created in period | 1 hr |
| | Trend badge logic | Each card shows +/- % compared to previous same-length period | 2 hrs |
| **TOTAL** | | | **10 hrs** |

### 3C. Charts — Row 1

| # | Chart | Details | Hours |
|---|-------|---------|-------|
| 3C.1 | Revenue Trend (All Labs) | Line chart, Y-axis in ₹ Lakhs/Crores, X-axis = daily dates in range, aggregate across all labs, time filter toggle | 5 hrs |
| 3C.2 | Revenue by Lab (Top 8) | Horizontal bar chart sorted descending, each bar = one lab's revenue in period, "View All" opens full table modal | 4 hrs |
| 3C.3 | Tests by Category | Donut chart — group `test_category` field from `lab_report`, show % + legend (Hematology, Biochemistry, Clinical Pathology, Immunology, Microbiology, Others) | 4 hrs |
| 3C.4 | Sample Status Overview | Donut chart — count visits by status: Pending, Collected, Partial Result, Completed. Show count + % in legend | 4 hrs |
| **TOTAL** | | | **17 hrs** |

### 3D. Lab Performance Summary Table

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 3D.1 | Table columns | Rank #, Lab Name, Revenue (₹), Tests, Patients, Pending Samples, Avg TAT, Reports Generated, Growth % (vs prev period), Action | 3 hrs |
| 3D.2 | Avg TAT calculation | Average of (report created_at - visit created_at) in hours, per lab | 3 hrs |
| 3D.3 | Growth % column | Compare current period revenue vs previous same period, color green/red | 2 hrs |
| 3D.4 | Pending Samples highlight | Color-code count cell: green <50, orange 50–200, red >200 | 1 hr |
| 3D.5 | Action button | Eye icon → navigate to Level 2 Lab Drill-down for that specific lab | 1 hr |
| 3D.6 | Table time filter | "This Week / This Month" toggle above table | 1 hr |
| **TOTAL** | | | **11 hrs** |

### 3E. Top Referring Doctors Table

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 3E.1 | Table columns | Rank #, Doctor Name, Labs count (how many labs they refer to), Patients count, Revenue (₹) | 2 hrs |
| 3E.2 | Data aggregation | Join visits → doctors across all labs, group by doctor, sum revenue from those visits | 3 hrs |
| 3E.3 | "View All Doctors" link | Modal/page showing complete list with same columns | 1 hr |
| 3E.4 | Time filter | "This Week / This Month" toggle | 1 hr |
| **TOTAL** | | | **7 hrs** |

### 3F. Alerts & Insights Panel

| # | Alert Type | Logic | Hours |
|---|-----------|-------|-------|
| 3F.1 | Reports delayed | Count reports where (current_time - visit_complete_time) > 4 hrs, group by lab count | 2 hrs |
| 3F.2 | Samples pending > 4 hrs | Count visits in PENDING/COLLECTED status older than 4 hrs | 2 hrs |
| 3F.3 | Critical reports awaiting review | Reports with status = PENDING_REVIEW or flag field = critical | 2 hrs |
| 3F.4 | Labs with low sample collection | Labs where sample count < threshold (configurable baseline) | 2 hrs |
| 3F.5 | Severity icon & color | Red = critical, Orange = warning, Blue = info | 1 hr |
| 3F.6 | "View All Alerts" page | Paginated list of all system alerts with filters | 3 hrs |
| **TOTAL** | | | **12 hrs** |

---

## 4. Lab Admin Dashboard — Single Lab View

**Access:** Lab Admin only — scoped to their assigned `labId`
**Route:** `/lab/{labId}/dashboard`
**Header tag:** "Level 2: Lab Drill-down"

### 4A. Header Section

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 4A.1 | Page title | "Lab Dashboard – {Lab Name}", "Level 2: Lab Drill-down" badge | 1 hr |
| 4A.2 | Date range | Same date picker component as super admin | 0.5 hr |
| 4A.3 | Download Report button | Export current dashboard view as PDF or CSV | 3 hrs |
| **TOTAL** | | | **4.5 hrs** |

### 4B. KPI Summary Cards (Top Row — 9 cards)

| # | Card | Data Source | Hours |
|---|------|-------------|-------|
| 4B.1 | Total Revenue | Sum of `billing.net_amount` for this lab in period | 1 hr |
| 4B.2 | Total Tests | Count test-results for this lab in period | 1 hr |
| 4B.3 | Total Patients | Count distinct patients who had visits in period | 1 hr |
| 4B.4 | Pending Samples | Count visits with PENDING status in this lab | 1 hr |
| 4B.5 | Reports Generated | Count reports in `lab_report` for this lab in period | 1 hr |
| 4B.6 | Avg TAT | Avg (report_created_at - visit_created_at) in hours for this lab | 2 hrs |
| 4B.7 | Active Admins | Count users with ADMIN role in this lab and `isActive = true` | 0.5 hr |
| 4B.8 | Desk Users | Count users with DESK_USER role in this lab | 0.5 hr |
| 4B.9 | Technicians | Count users with TECHNICIAN role in this lab | 0.5 hr |
| | Trend badge logic | +/- vs previous same-length period for cards 1–6 | 2 hrs |
| **TOTAL** | | | **10.5 hrs** |

### 4C. Charts — Row 1

| # | Chart | Details | Hours |
|---|-------|---------|-------|
| 4C.1 | Daily Revenue Trend | Line chart — daily revenue for this lab in date range, Y-axis ₹, time filter | 4 hrs |
| 4C.2 | Sample Workflow Funnel | 5-step funnel: Registered → Collected → Results Entered → Reports Generated → Reports Delivered. Show count + % conversion per step | 6 hrs |
| 4C.3 | Tests by Category | Donut chart — same structure as super admin but scoped to this lab | 3 hrs |
| **TOTAL** | | | **13 hrs** |

### 4D. Charts — Row 2

| # | Chart | Details | Hours |
|---|-------|---------|-------|
| 4D.1 | Top Ordered Tests | Horizontal bar chart — top 5 tests by count for this lab (test name + count), "View All Tests" link | 4 hrs |
| 4D.2 | Revenue by Collection Method | Donut chart — group `transaction.payment_method` (UPI, Cash, Card, Credit), show % + ₹ amount | 4 hrs |
| **TOTAL** | | | **8 hrs** |

### 4E. Technician Performance Table

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 4E.1 | Table columns | Technician Name, Samples Processed, Reports Entered, Avg TAT | 2 hrs |
| 4E.2 | Data aggregation | Group visits/reports by `created_by` user filtered to TECHNICIAN role in this lab | 3 hrs |
| 4E.3 | "View All Technicians" link | Full list with same columns | 1 hr |
| **TOTAL** | | | **6 hrs** |

### 4F. Top Referring Doctors Table

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 4F.1 | Table columns | Doctor Name, Patients count, Revenue (₹) | 1 hr |
| 4F.2 | Data query | Group visits by `doctor_id` for this lab, count patients, sum billing revenue | 2 hrs |
| 4F.3 | "View All Doctors" link | Modal/page with full list | 1 hr |
| **TOTAL** | | | **4 hrs** |

### 4G. Package Performance Table

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 4G.1 | Table columns | Package Name, Bookings count, Revenue (₹) | 1 hr |
| 4G.2 | Data query | Group `visit_packages` by package_id for this lab, count bookings, sum revenue | 3 hrs |
| 4G.3 | "View All Packages" link | Full list modal | 1 hr |
| **TOTAL** | | | **5 hrs** |

### 4H. Age & Gender Distribution

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 4H.1 | Gender pie chart | Male / Female / Other — count from `patient.gender` for visits in period | 3 hrs |
| 4H.2 | Age group bar chart | Vertical bars: 0–18, 19–35, 36–50, 51–65, 65+ — calculated from `patient.date_of_birth` | 3 hrs |
| **TOTAL** | | | **6 hrs** |

### 4I. Recent Alerts (This Lab)

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 4I.1 | Alert items | Show top 3 recent alerts: reports delayed, samples pending, critical reports | 2 hrs |
| 4I.2 | "View" link per alert | Navigate to relevant list (reports/samples) filtered to that issue | 1 hr |
| 4I.3 | "View All Alerts" link | Full alerts list scoped to this lab | 1 hr |
| **TOTAL** | | | **4 hrs** |

---

## 5. Backend APIs — Super Admin

All endpoints under `/super-admin/dashboard/**`
Security: `SUPER_ADMIN` role required.

| # | Endpoint | Description | Query Params | Hours |
|---|----------|-------------|--------------|-------|
| 5.1 | `GET /super-admin/dashboard/kpi-summary` | Returns all 8 KPI card values + trend % for each | `from`, `to` (date range) | 4 hrs |
| 5.2 | `GET /super-admin/dashboard/revenue-trend` | Daily revenue totals across all labs for date range | `from`, `to`, `interval` (daily/weekly) | 4 hrs |
| 5.3 | `GET /super-admin/dashboard/revenue-by-lab` | Top N labs by revenue in period | `from`, `to`, `limit` (default 8) | 3 hrs |
| 5.4 | `GET /super-admin/dashboard/tests-by-category` | Count of tests grouped by `test_category` | `from`, `to` | 3 hrs |
| 5.5 | `GET /super-admin/dashboard/sample-status-overview` | Count of visits grouped by status | `from`, `to` | 2 hrs |
| 5.6 | `GET /super-admin/dashboard/lab-performance-summary` | Table data: all labs with revenue, tests, patients, pending samples, avg TAT, reports, growth % | `from`, `to`, `page`, `size` | 6 hrs |
| 5.7 | `GET /super-admin/dashboard/top-referring-doctors` | Top doctors by patient count + revenue across all labs | `from`, `to`, `limit` | 4 hrs |
| 5.8 | `GET /super-admin/dashboard/alerts` | Active system alerts with type, count, severity | — | 4 hrs |
| **TOTAL** | | | | **30 hrs** |

---

## 6. Backend APIs — Lab Admin

All endpoints under `/lab/{labId}/dashboard/**`
Security: `LAB_ADMIN` role + user must be member of `labId`.

| # | Endpoint | Description | Query Params | Hours |
|---|----------|-------------|--------------|-------|
| 6.1 | `GET /lab/{labId}/dashboard/kpi-summary` | Returns all 9 KPI card values + trends for this lab | `from`, `to` | 4 hrs |
| 6.2 | `GET /lab/{labId}/dashboard/revenue-trend` | Daily revenue for this lab | `from`, `to` | 3 hrs |
| 6.3 | `GET /lab/{labId}/dashboard/sample-funnel` | 5-step funnel counts: Registered, Collected, Results Entered, Reports Generated, Delivered | `from`, `to` | 4 hrs |
| 6.4 | `GET /lab/{labId}/dashboard/tests-by-category` | Tests grouped by category for this lab | `from`, `to` | 2 hrs |
| 6.5 | `GET /lab/{labId}/dashboard/top-ordered-tests` | Top 5 tests by volume for this lab | `from`, `to`, `limit` | 3 hrs |
| 6.6 | `GET /lab/{labId}/dashboard/revenue-by-payment-method` | Revenue grouped by UPI/Cash/Card/Credit | `from`, `to` | 3 hrs |
| 6.7 | `GET /lab/{labId}/dashboard/technician-performance` | Per-technician: samples processed, reports entered, avg TAT | `from`, `to` | 4 hrs |
| 6.8 | `GET /lab/{labId}/dashboard/top-referring-doctors` | Top doctors by patient + revenue for this lab | `from`, `to`, `limit` | 3 hrs |
| 6.9 | `GET /lab/{labId}/dashboard/package-performance` | Package bookings + revenue for this lab | `from`, `to` | 3 hrs |
| 6.10 | `GET /lab/{labId}/dashboard/age-gender-distribution` | Patient age groups + gender split | `from`, `to` | 3 hrs |
| 6.11 | `GET /lab/{labId}/dashboard/alerts` | Active alerts scoped to this lab | — | 3 hrs |
| **TOTAL** | | | | **35 hrs** |

---

## 7. Filters & Date Range Logic

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 7.1 | Date range filter component | Calendar picker with From/To, preset buttons (Today, This Week, This Month, Last Month, Custom) | 4 hrs |
| 7.2 | Backend date filtering | All queries accept `from` and `to` as ISO date params, default = last 7 days | 2 hrs |
| 7.3 | Trend comparison logic | Auto-compute previous period of same length to calculate +/- % change | 3 hrs |
| 7.4 | "This Week / This Month" chart toggle | Small dropdown on chart header re-fetches data with new date range | 2 hrs |
| 7.5 | Filter panel — Super Admin | Filter by: city, lab type (diagnostic/collection), active/inactive labs | 3 hrs |
| 7.6 | Filter state management | Persist selected filter in URL query params so page is shareable/bookmarkable | 2 hrs |
| **TOTAL** | | | **16 hrs** |

---

## 8. Alerts & Insights Engine

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 8.1 | Report delay detection | Query: `lab_report` where `created_at - visit.created_at > 4 hrs` AND `report_status != DELIVERED` | 3 hrs |
| 8.2 | Sample pending > 4 hrs | Query: `patient_visits` where `visit_status IN (PENDING, COLLECTED)` AND `created_at < now - 4 hrs` | 2 hrs |
| 8.3 | Critical reports awaiting review | Query reports where `report_status = PENDING_REVIEW` | 2 hrs |
| 8.4 | Low sample collection | Compare current period sample count per lab vs 30-day rolling average, flag if < 70% | 3 hrs |
| 8.5 | Alert severity levels | CRITICAL (red), WARNING (orange), INFO (blue) — stored as enum on alert record | 1 hr |
| 8.6 | Alert persistence | `lab_alerts` table: id, lab_id, type, message, severity, is_resolved, created_at | 3 hrs |
| 8.7 | Scheduled alert job | Spring `@Scheduled` task runs every 30 min to detect and insert new alerts | 3 hrs |
| 8.8 | Mark alert resolved | API: `PUT /lab/{labId}/alerts/{alertId}/resolve` | 1 hr |
| **TOTAL** | | | **18 hrs** |

---

## 9. Download Report Feature

Available on Lab Admin Dashboard only (per the screenshot).

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 9.1 | Download button UI | "Download Report" button in header, dropdown: PDF / CSV | 2 hrs |
| 9.2 | PDF export | Generate PDF of current dashboard view: KPI summary + tables (use iText or JasperReports on backend, or html2canvas + jsPDF on frontend) | 6 hrs |
| 9.3 | CSV export | Export lab performance data as `.csv` file — date range, all KPIs, table rows | 4 hrs |
| 9.4 | Backend export endpoint | `GET /lab/{labId}/dashboard/export?format=pdf|csv&from=&to=` | 3 hrs |
| **TOTAL** | | | **15 hrs** |

---

## 10. Testing

| # | Sub-Task | Details | Hours |
|---|----------|---------|-------|
| 10.1 | Unit test — KPI aggregation queries | Verify revenue sums, count logic, trend % calculation with test data | 4 hrs |
| 10.2 | Unit test — alert detection | Simulate delayed reports and pending samples, verify alert creation | 3 hrs |
| 10.3 | Integration test — Super Admin APIs | All 8 endpoints return correct shape and data with role auth | 4 hrs |
| 10.4 | Integration test — Lab Admin APIs | All 11 endpoints scoped correctly to labId, blocked for wrong lab | 4 hrs |
| 10.5 | Frontend component testing | Chart renders, KPI cards, table sort/paginate | 3 hrs |
| 10.6 | Role access test | Super Admin cannot hit `/lab/{id}/dashboard`, Lab Admin cannot hit `/super-admin/dashboard` | 2 hrs |
| 10.7 | Date range edge cases | Empty date range, single day, large range (1 year), missing data periods | 2 hrs |
| **TOTAL** | | | **22 hrs** |

---

## 11. Hour Summary Table

| Module | Area | Hours |
|--------|------|-------|
| 1 | Architecture & Setup | 20 hrs |
| 2 | Shared UI Components | 29 hrs |
| 3A–3B | Super Admin Header + KPI Cards | 11 hrs |
| 3C | Super Admin Charts Row 1 | 17 hrs |
| 3D | Lab Performance Summary Table | 11 hrs |
| 3E | Top Referring Doctors (SA) | 7 hrs |
| 3F | Alerts & Insights Panel (SA) | 12 hrs |
| 4A–4B | Lab Admin Header + KPI Cards | 14.5 hrs |
| 4C | Lab Admin Charts Row 1 | 13 hrs |
| 4D | Lab Admin Charts Row 2 | 8 hrs |
| 4E | Technician Performance Table | 6 hrs |
| 4F | Top Referring Doctors (LA) | 4 hrs |
| 4G | Package Performance Table | 5 hrs |
| 4H | Age & Gender Distribution | 6 hrs |
| 4I | Recent Alerts (Lab) | 4 hrs |
| 5 | Backend APIs — Super Admin | 30 hrs |
| 6 | Backend APIs — Lab Admin | 35 hrs |
| 7 | Filters & Date Range Logic | 16 hrs |
| 8 | Alerts & Insights Engine | 18 hrs |
| 9 | Download Report Feature | 15 hrs |
| 10 | Testing | 22 hrs |
| **GRAND TOTAL** | | **~303 hrs** |

---

## Delivery Estimate (2 developers)

| Sprint | Work | Est. Duration |
|--------|------|---------------|
| Sprint 1 | Setup + Shared Components + All Backend APIs | ~2.5 weeks |
| Sprint 2 | Super Admin Dashboard (all sections) | ~2 weeks |
| Sprint 3 | Lab Admin Dashboard (all sections) | ~2 weeks |
| Sprint 4 | Alerts Engine + Download + Filters + Testing | ~1.5 weeks |
| **Total** | | **~8 weeks** |

> Single developer: ~14–15 weeks
> With 1 backend + 1 frontend developer working in parallel: ~6–7 weeks

---

## Quick Reference — Data Sources per Widget

| Widget | Table(s) | Key Columns |
|--------|----------|-------------|
| Total Revenue | `billing` | `net_amount`, `created_at`, `lab_id` (via visit) |
| Total Tests | `patient_visit_tests` | `visit_id`, `test_id` |
| Total Patients | `patients`, `patient_visits` | `patient_id`, `lab_id`, `visit_date` |
| Pending Samples | `patient_visits` | `visit_status`, `lab_id` |
| Reports Generated | `lab_report` | `lab_id`, `created_at` |
| Avg TAT | `patient_visits`, `lab_report` | `created_at` diff in hours |
| Revenue Trend | `billing`, `patient_visits` | `net_amount`, `visit_date`, grouped by day |
| Tests by Category | `lab_report` | `test_category`, `lab_id` |
| Sample Funnel | `patient_visits`, `visit_sample`, `lab_report` | `visit_status`, `created_at` |
| Top Tests | `patient_visit_tests`, `tests` | `test_name`, count |
| Payment Method | `billing_transaction` | `payment_method`, `received_amount` |
| Technician Perf | `lab_report`, `users` | `created_by` (TECHNICIAN role) |
| Top Doctors | `patient_visits`, `doctors` | `doctor_id`, billing join |
| Package Perf | `patient_visits` (packageIds), `billing` | package join, `net_amount` |
| Age & Gender | `patients` | `date_of_birth`, `gender` |
| Alerts | `lab_alerts` (new table) | `type`, `severity`, `lab_id`, `is_resolved` |
