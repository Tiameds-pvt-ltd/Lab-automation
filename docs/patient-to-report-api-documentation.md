# Patient to Report Generation — Detailed API Documentation

**Project:** Lab Automation
**Module:** Patient → Report Flow
**Date:** 2026-06-29

---

## Table of Contents

1. [Flow Overview](#flow-overview)
2. [Phase 1 — Patient Management](#phase-1--patient-management)
3. [Phase 2 — Visit Management](#phase-2--visit-management)
4. [Phase 3 — Sample Collection](#phase-3--sample-collection)
5. [Phase 4A — Test Management](#phase-4a--test-management)
6. [Phase 4B — Test Reference Values](#phase-4b--test-reference-values)
7. [Phase 5 — Result Entry & Report Generation](#phase-5--result-entry--report-generation)
8. [Phase 6 — Report Settings / Template](#phase-6--report-settings--template)
9. [Phase 7 — Billing & Payments](#phase-7--billing--payments)
10. [Phase 8 — Sample Type Configuration](#phase-8--sample-type-configuration)
11. [Hour Estimate Summary](#hour-estimate-summary)

---

## Flow Overview

```
Patient Registration
        ↓
   Create Visit  ←→  Billing Auto-Created
        ↓
 Sample Collection  (mark visit as Collected)
        ↓
 Result / Report Entry  (enter test values)
        ↓
   Complete Visit  (finalize all reports)
        ↓
  View / Print Report
```

---

## Phase 1 — Patient Management

**Controller:** `PatientController.java`
**Base Path:** `/lab/{labId}`

---

### Endpoint 1 — List All Patients

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/patients` |
| **Description** | Fetch all patients registered under a specific lab |
| **Task** | List Patients |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token and extract user identity | 0.5 |
| ST-2 | Validate `labId` exists and belongs to authenticated user | 0.5 |
| ST-3 | Query patient table filtered by `labId` with pagination support | 1 |
| ST-4 | Map entity to response DTO | 0.5 |
| ST-5 | Return paginated response with metadata (page, size, total) | 0.5 |
| **Total** | | **3** |

---

### Endpoint 2 — Get Patient by ID

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/patient/{patientId}` |
| **Description** | Get a single patient's full details by ID |
| **Task** | Get Patient by ID |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` exists and is accessible | 0.25 |
| ST-3 | Validate `patientId` belongs to the given lab | 0.5 |
| ST-4 | Fetch patient record with related visits | 0.5 |
| ST-5 | Map to response DTO and return | 0.5 |
| **Total** | | **2** |

---

### Endpoint 3 — Register New Patient (or Add Visit to Existing)

| Field | Details |
|-------|---------|
| **Method** | `POST` |
| **URL** | `/lab/{labId}/add-patient` |
| **Description** | Register a new patient OR add a new visit/billing to an existing patient |
| **Task** | Register Patient |
| **Total Hours** | 8 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate and validate JWT | 0.5 |
| ST-2 | Validate `labId` exists and is accessible | 0.5 |
| ST-3 | Validate required fields: name, phone, age, gender | 0.5 |
| ST-4 | Check if patient already exists by phone number in same lab | 0.5 |
| ST-5 | If new patient: create patient record with all fields | 1 |
| ST-6 | If existing patient: fetch existing record and prepare new visit | 0.5 |
| ST-7 | Create new visit record linked to patient and lab | 1 |
| ST-8 | Validate and attach test list to the visit | 0.5 |
| ST-9 | Auto-generate billing record from selected tests (sum of test prices) | 1 |
| ST-10 | Set initial visit status to PENDING | 0.25 |
| ST-11 | Set initial billing status to UNPAID | 0.25 |
| ST-12 | Save all entities in a single transaction | 0.5 |
| ST-13 | Return full patient + visit + billing response | 0.5 |
| ST-14 | Handle error: duplicate phone same lab (return existing patient suggestion) | 0.5 |
| **Total** | | **8** |

---

### Endpoint 4 — Update Patient Basic Info

| Field | Details |
|-------|---------|
| **Method** | `PUT` |
| **URL** | `/lab/{labId}/update-patient/{patientId}` |
| **Description** | Update basic patient information (name, age, gender, phone) |
| **Task** | Update Patient Basic Info |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `patientId` ownership | 0.5 |
| ST-3 | Validate incoming fields (no empty name, valid phone format) | 0.5 |
| ST-4 | Check phone uniqueness if phone is being changed | 0.5 |
| ST-5 | Update allowed fields only (name, age, gender, phone) | 0.5 |
| ST-6 | Persist changes and return updated patient | 0.5 |
| ST-7 | Handle error: patient not found or not in lab | 0.25 |
| **Total** | | **3** |

---

### Endpoint 5 — Update Patient Detailed Info (with Field Tracking)

| Field | Details |
|-------|---------|
| **Method** | `PUT` |
| **URL** | `/lab/{labId}/update-patient-details/{patientId}` |
| **Description** | Update detailed patient info with field-level change tracking / audit log |
| **Task** | Update Patient Details |
| **Total Hours** | 5 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `patientId` ownership | 0.5 |
| ST-3 | Fetch existing patient snapshot before update | 0.5 |
| ST-4 | Validate incoming detailed fields (address, email, DOB, ref doctor, etc.) | 0.5 |
| ST-5 | Compare old vs new values field by field | 0.5 |
| ST-6 | Build change log with: field name, old value, new value, updated by, timestamp | 1 |
| ST-7 | Apply updates to patient entity | 0.5 |
| ST-8 | Persist patient record and audit log together in a transaction | 0.5 |
| ST-9 | Return updated patient with applied changes | 0.25 |
| ST-10 | Handle error: no changes detected (return 304 or informative message) | 0.5 |
| **Total** | | **5** |

---

### Endpoint 6 — Delete Patient

| Field | Details |
|-------|---------|
| **Method** | `DELETE` |
| **URL** | `/lab/{labId}/delete-patient/{patientId}` |
| **Description** | Permanently delete a patient and all associated data |
| **Task** | Delete Patient |
| **Total Hours** | 4 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `patientId` ownership | 0.5 |
| ST-3 | Check for associated active visits (warn or block if any are in progress) | 0.5 |
| ST-4 | Cascade delete: visit samples | 0.5 |
| ST-5 | Cascade delete: reports linked to visits | 0.5 |
| ST-6 | Cascade delete: billing records | 0.5 |
| ST-7 | Delete patient record | 0.25 |
| ST-8 | Return success response with deletion summary | 0.25 |
| ST-9 | Handle error: patient not found | 0.25 |
| ST-10 | Handle error: patient has completed/locked visits (deny delete) | 0.5 |
| **Total** | | **4** |

---

### Endpoint 7 — Search Patient by Phone

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/search-patient` |
| **Description** | Search patient by phone number within a lab |
| **Task** | Search Patient |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `phone` query param presence | 0.25 |
| ST-3 | Query patient table: WHERE lab_id = labId AND phone LIKE %phone% | 0.5 |
| ST-4 | Return list of matching patients with basic details | 0.5 |
| ST-5 | Handle empty result (return empty list, not 404) | 0.5 |
| **Total** | | **2** |

---

### Endpoint 8 — Cancel Patient Visit

| Field | Details |
|-------|---------|
| **Method** | `PUT` |
| **URL** | `/lab/{labId}/visit/{visitId}/cancel` |
| **Description** | Cancel a specific patient visit |
| **Task** | Cancel Visit |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `visitId` ownership | 0.5 |
| ST-3 | Check current visit status (cannot cancel already COMPLETED visits) | 0.5 |
| ST-4 | Update visit status to CANCELLED with cancellation timestamp | 0.5 |
| ST-5 | Update linked billing status to CANCELLED | 0.5 |
| ST-6 | Void any associated reports | 0.5 |
| ST-7 | Return success response | 0.25 |
| **Total** | | **3** |

**Phase 1 Total Hours: 30**

---

## Phase 2 — Visit Management

**Controller:** `VisitController.java`
**Base Path:** `/lab/{labId}`

---

### Endpoint 9 — Create Visit for Patient

| Field | Details |
|-------|---------|
| **Method** | `POST` |
| **URL** | `/lab/{labId}/add-visit/{patientId}` |
| **Description** | Create a new lab visit for an existing patient |
| **Task** | Create Visit |
| **Total Hours** | 5 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `patientId` exist and are linked | 0.5 |
| ST-3 | Validate visit payload: tests, doctor name, visit date | 0.5 |
| ST-4 | Validate each test ID belongs to the lab | 0.5 |
| ST-5 | Create visit record with status PENDING | 0.5 |
| ST-6 | Link selected tests to the visit | 0.5 |
| ST-7 | Auto-generate billing record from test prices | 1 |
| ST-8 | Persist visit and billing in one transaction | 0.5 |
| ST-9 | Return visit + billing details in response | 0.5 |
| ST-10 | Handle error: invalid test IDs or lab mismatch | 0.25 |
| **Total** | | **5** |

---

### Endpoint 10 — Get All Visits

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/visits` |
| **Description** | Retrieve all visits under a lab |
| **Task** | Get All Visits |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Query visits by `labId` with pagination (page, size params) | 0.75 |
| ST-4 | Include patient name, visit date, status in response | 0.75 |
| ST-5 | Return paginated list with total count | 0.5 |
| ST-6 | Handle empty result gracefully | 0.5 |
| **Total** | | **3** |

---

### Endpoint 11 — Get Visit by ID

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/visit/{visitId}` |
| **Description** | Get full details of a specific visit |
| **Task** | Get Visit by ID |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `visitId` ownership | 0.5 |
| ST-3 | Fetch visit with associated patient, tests, samples, billing | 0.75 |
| ST-4 | Map to full response DTO and return | 0.5 |
| **Total** | | **2** |

---

### Endpoint 12 — Get Visits by Patient

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/patient/{patientId}/visit` |
| **Description** | Get all visits for a particular patient |
| **Task** | Get Patient Visits |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `patientId` | 0.5 |
| ST-3 | Query visits filtered by patientId and labId | 0.75 |
| ST-4 | Return list ordered by visit date descending | 0.5 |
| **Total** | | **2** |

---

### Endpoint 13 — Update Visit

| Field | Details |
|-------|---------|
| **Method** | `PUT` |
| **URL** | `/lab/{labId}/update-visit/{visitId}` |
| **Description** | Update visit metadata (doctor, date, notes, tests) |
| **Task** | Update Visit |
| **Total Hours** | 4 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `visitId` ownership | 0.5 |
| ST-3 | Reject update if visit status is COMPLETED or CANCELLED | 0.5 |
| ST-4 | Validate updated fields: doctor, notes, visit date | 0.5 |
| ST-5 | If tests changed: recalculate billing amount | 1 |
| ST-6 | Update visit record | 0.5 |
| ST-7 | Persist changes and return updated visit | 0.5 |
| ST-8 | Handle error: immutable completed visit | 0.25 |
| **Total** | | **4** |

---

### Endpoint 14 — Delete Visit

| Field | Details |
|-------|---------|
| **Method** | `DELETE` |
| **URL** | `/lab/{labId}/delete-visit/{visitId}` |
| **Description** | Delete a specific visit |
| **Task** | Delete Visit |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `visitId` ownership | 0.5 |
| ST-3 | Check visit status (block if COMPLETED) | 0.5 |
| ST-4 | Delete associated samples | 0.5 |
| ST-5 | Delete associated reports | 0.5 |
| ST-6 | Delete billing record | 0.25 |
| ST-7 | Delete visit record and return success | 0.5 |
| **Total** | | **3** |

---

### Endpoint 15 — Visits by Date Range

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/visitsdatewise` |
| **Description** | Get all visits filtered by a date range |
| **Task** | Visits by Date Range |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId`, `startDate`, `endDate` query params | 0.5 |
| ST-3 | Parse and validate date format (ISO 8601) | 0.5 |
| ST-4 | Query visits WHERE visit_date BETWEEN startDate AND endDate | 0.75 |
| ST-5 | Return filtered list with patient and billing summary | 0.5 |
| ST-6 | Handle error: invalid date range (startDate > endDate) | 0.5 |
| **Total** | | **3** |

---

### Endpoint 16 — Lab Visits Organized by Date

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/datewise-lab-visits` |
| **Description** | Get lab visits by date range grouped by date |
| **Task** | Lab Visits by Date |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId`, `startDate`, `endDate` | 0.5 |
| ST-3 | Query and fetch visits in date range | 0.5 |
| ST-4 | Group visits by date key (e.g., "2026-06-29": [...]) | 0.75 |
| ST-5 | Include count per date in response | 0.5 |
| ST-6 | Return structured date-keyed JSON response | 0.5 |
| **Total** | | **3** |

---

### Endpoint 17 — Patient Visits by Date Range

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/datewise-patient-visits` |
| **Description** | Get patient visits filtered by date range |
| **Task** | Patient Visits by Date |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId`, `startDate`, `endDate` | 0.5 |
| ST-3 | Query patient-linked visits in date range | 0.75 |
| ST-4 | Include patient name, phone, visit status per record | 0.5 |
| ST-5 | Return list sorted by visit_date ascending | 0.5 |
| ST-6 | Handle empty result | 0.5 |
| **Total** | | **3** |

---

### Endpoint 18 — Delete Patient Visit (Cascade)

| Field | Details |
|-------|---------|
| **Method** | `DELETE` |
| **URL** | `/lab/{labId}/delete-patient-visit/{visitId}` |
| **Description** | Delete a patient visit along with all related data |
| **Task** | Delete Patient Visit |
| **Total Hours** | 4 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `visitId` ownership | 0.5 |
| ST-3 | Block deletion if visit is COMPLETED | 0.5 |
| ST-4 | Delete all samples linked to visit | 0.5 |
| ST-5 | Delete all report entries linked to visit | 0.5 |
| ST-6 | Delete billing record linked to visit | 0.5 |
| ST-7 | Delete visit record | 0.25 |
| ST-8 | Return cascade deletion summary | 0.5 |
| ST-9 | Handle partial failure with rollback | 0.5 |
| **Total** | | **4** |

---

### Endpoint 19 — Delete All Visits for Lab

| Field | Details |
|-------|---------|
| **Method** | `DELETE` |
| **URL** | `/lab/{labId}/delete-all-patient-visits` |
| **Description** | Delete all visits for a lab (bulk operation) |
| **Task** | Delete All Visits |
| **Total Hours** | 5 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token (admin-level permission required) | 0.5 |
| ST-2 | Validate `labId` | 0.5 |
| ST-3 | Confirm operation (require confirmation param or body flag) | 0.5 |
| ST-4 | Fetch all visit IDs for lab | 0.5 |
| ST-5 | Bulk delete all samples linked to those visits | 0.5 |
| ST-6 | Bulk delete all reports linked to those visits | 0.5 |
| ST-7 | Bulk delete all billing records | 0.5 |
| ST-8 | Bulk delete all visit records | 0.5 |
| ST-9 | Write audit log entry for bulk deletion | 0.5 |
| ST-10 | Return summary: count of deleted records per entity | 0.5 |
| **Total** | | **5** |

**Phase 2 Total Hours: 37**

---

## Phase 3 — Sample Collection

**Controller:** `PatientVisitSample.java`
**Base Path:** `/lab`

---

### Endpoint 20 — Add Samples to Visit

| Field | Details |
|-------|---------|
| **Method** | `POST` |
| **URL** | `/lab/add-samples` |
| **Description** | Add collected samples to a visit; marks visit status as "Collected" |
| **Task** | Add Samples to Visit |
| **Total Hours** | 5 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `visitId` in request body | 0.5 |
| ST-3 | Validate each `sampleTypeId` exists and belongs to the lab | 0.5 |
| ST-4 | Check visit status is PENDING (cannot add samples to cancelled/completed visits) | 0.5 |
| ST-5 | Create sample collection records for each sample type | 1 |
| ST-6 | Set collection timestamp and collected-by user | 0.5 |
| ST-7 | Update visit status from PENDING → COLLECTED | 0.5 |
| ST-8 | Persist all changes in one transaction | 0.5 |
| ST-9 | Return updated visit with sample details | 0.5 |
| ST-10 | Handle error: duplicate sample type for same visit | 0.25 |
| **Total** | | **5** |

---

### Endpoint 21 — Update Samples

| Field | Details |
|-------|---------|
| **Method** | `PUT` |
| **URL** | `/lab/update-samples` |
| **Description** | Update sample information associated with a visit |
| **Task** | Update Samples |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `visitId` and `sampleId` list in request | 0.5 |
| ST-3 | Check visit is not COMPLETED or CANCELLED | 0.5 |
| ST-4 | Update sample type, quantity, or notes per sample record | 0.75 |
| ST-5 | Persist changes | 0.5 |
| ST-6 | Return updated sample list | 0.5 |
| **Total** | | **3** |

---

### Endpoint 22 — Delete Samples

| Field | Details |
|-------|---------|
| **Method** | `DELETE` |
| **URL** | `/lab/delete-samples` |
| **Description** | Remove one or more samples from a visit |
| **Task** | Delete Samples |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `visitId` and list of `sampleId` to delete | 0.5 |
| ST-3 | Check no reports have already been entered for those samples | 0.5 |
| ST-4 | Delete sample records | 0.5 |
| ST-5 | If no samples remain: revert visit status back to PENDING | 0.5 |
| ST-6 | Persist changes and return updated visit status | 0.75 |
| **Total** | | **3** |

---

### Endpoint 23 — Get Collected/Completed Patients

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/patients/collected-completed` |
| **Description** | Get all patients with Collected or Completed visit status, filtered by date range |
| **Task** | Get Collected/Completed Patients |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId`, `startDate`, `endDate` | 0.5 |
| ST-3 | Query visits with status IN (COLLECTED, COMPLETED) within date range | 0.75 |
| ST-4 | Join with patient details | 0.5 |
| ST-5 | Return list with patient name, phone, visit status, date | 0.5 |
| ST-6 | Handle empty result | 0.5 |
| **Total** | | **3** |

**Phase 3 Total Hours: 14**

---

## Phase 4A — Test Management

**Controller:** `TestController.java`
**Base Path:** `/admin/lab`

---

### Endpoint 24 — List Tests (Paginated, by Query Param)

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/admin/lab/tests?labId={labId}` |
| **Description** | Get all tests (paginated) filtered by labId query param |
| **Task** | List Tests Paginated |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` query param | 0.25 |
| ST-3 | Query tests by labId with pagination | 0.75 |
| ST-4 | Return paginated response (tests, page info) | 0.75 |
| **Total** | | **2** |

---

### Endpoint 25 — List All Tests for Lab

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/admin/lab/{labId}/tests` |
| **Description** | Get all tests for a specific lab (no pagination) |
| **Task** | List Lab Tests |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` path variable | 0.25 |
| ST-3 | Query all tests for lab | 0.75 |
| ST-4 | Return full list with test code, name, price, category | 0.75 |
| **Total** | | **2** |

---

### Endpoint 26 — Create Test

| Field | Details |
|-------|---------|
| **Method** | `POST` |
| **URL** | `/admin/lab/{labId}/add` |
| **Description** | Add a new test to the lab with auto-generated unique test code |
| **Task** | Create Test |
| **Total Hours** | 5 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Validate required fields: test name, price, category, unit, sample type | 0.75 |
| ST-4 | Check for duplicate test name within same lab | 0.5 |
| ST-5 | Auto-generate unique test code (e.g., LAB-CBC-001) | 1 |
| ST-6 | Save test record to database | 0.5 |
| ST-7 | Return created test with assigned code | 0.25 |
| ST-8 | Handle error: duplicate test name | 0.5 |
| ST-9 | Handle error: invalid category or sample type | 0.5 |
| ST-10 | Unit test: code generation uniqueness | 0.5 |
| **Total** | | **5** |

---

### Endpoint 27 — Update Test

| Field | Details |
|-------|---------|
| **Method** | `PUT` |
| **URL** | `/admin/lab/{labId}/update/{testId}` |
| **Description** | Update existing test details |
| **Task** | Update Test |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `testId` ownership | 0.5 |
| ST-3 | Validate updated fields (name, price, unit, category) | 0.5 |
| ST-4 | Check duplicate name if name is being changed | 0.5 |
| ST-5 | Update test record (test code is immutable) | 0.5 |
| ST-6 | Return updated test details | 0.25 |
| ST-7 | Handle error: test not found or lab mismatch | 0.5 |
| **Total** | | **3** |

---

### Endpoint 28 — Get Test by ID

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/admin/lab/{labId}/test/{testId}` |
| **Description** | Get full details of a specific test |
| **Task** | Get Test by ID |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `testId` | 0.5 |
| ST-3 | Fetch test with reference values if available | 0.75 |
| ST-4 | Return full test details DTO | 0.5 |
| **Total** | | **2** |

---

### Endpoint 29 — Delete Test

| Field | Details |
|-------|---------|
| **Method** | `DELETE` |
| **URL** | `/admin/lab/{labId}/remove/{testId}` |
| **Description** | Remove a test from the lab |
| **Task** | Delete Test |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `testId` | 0.5 |
| ST-3 | Check if test is linked to any PENDING or COLLECTED visit (block if yes) | 1 |
| ST-4 | Delete test record | 0.25 |
| ST-5 | Delete associated test reference values | 0.5 |
| ST-6 | Return success or error with reason | 0.5 |
| **Total** | | **3** |

---

### Endpoint 30 — Bulk Upload Tests via CSV

| Field | Details |
|-------|---------|
| **Method** | `POST` |
| **URL** | `/admin/lab/test/{labId}/csv/upload` |
| **Description** | Bulk import tests from a CSV file |
| **Task** | Bulk Upload Tests via CSV |
| **Total Hours** | 6 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Validate uploaded file is CSV and within size limit | 0.5 |
| ST-4 | Parse CSV: read headers and rows | 1 |
| ST-5 | Validate each row: required columns, data types, price format | 1 |
| ST-6 | Check for duplicate test names per row vs. existing DB records | 0.75 |
| ST-7 | Auto-generate unique test codes for each valid row | 0.5 |
| ST-8 | Bulk insert valid records in batches | 0.75 |
| ST-9 | Collect failed rows with error reasons | 0.5 |
| ST-10 | Return upload summary: total rows, inserted, skipped, errors | 0.5 |
| **Total** | | **6** |

---

### Endpoint 31 — Download Tests as CSV

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/admin/lab/{labId}/download` |
| **Description** | Export all tests for a lab as a downloadable CSV file |
| **Task** | Download Tests CSV |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Fetch all tests for lab from database | 0.5 |
| ST-4 | Map test fields to CSV columns (code, name, price, unit, category) | 0.75 |
| ST-5 | Write to CSV byte stream with headers | 0.5 |
| ST-6 | Set Content-Disposition header for file download | 0.25 |
| ST-7 | Return CSV file stream in response | 0.5 |
| **Total** | | **3** |

**Phase 4A Total Hours: 29**

---

## Phase 4B — Test Reference Values

**Controller:** `TestReferenceController.java`
**Base Path:** `/lab/test-reference`

---

### Endpoint 32 — List Test References (Paginated)

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/test-reference?labId={labId}` |
| **Description** | Get all test reference values (paginated) for a lab |
| **Task** | List Test References |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` query param | 0.25 |
| ST-3 | Query test_reference table filtered by labId with pagination | 0.75 |
| ST-4 | Return paginated list with reference ranges per test | 0.75 |
| **Total** | | **2** |

---

### Endpoint 33 — Bulk Upload References via CSV

| Field | Details |
|-------|---------|
| **Method** | `POST` |
| **URL** | `/lab/test-reference/{labId}/csv/upload` |
| **Description** | Bulk upload test reference values via CSV |
| **Task** | Bulk Upload References |
| **Total Hours** | 6 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Validate CSV file format and size | 0.5 |
| ST-4 | Parse CSV: read test code, min value, max value, unit, gender, age group | 1 |
| ST-5 | Validate each row: test code must exist in lab, numeric ranges | 1 |
| ST-6 | Check for duplicate references (same test + gender + age group) | 0.75 |
| ST-7 | Bulk insert valid rows | 0.75 |
| ST-8 | Collect and report invalid/skipped rows with reasons | 0.5 |
| ST-9 | Return upload summary (inserted, skipped, errors) | 0.5 |
| ST-10 | Handle encoding issues in CSV | 0.5 |
| **Total** | | **6** |

---

### Endpoint 34 — Delete All References for Lab

| Field | Details |
|-------|---------|
| **Method** | `DELETE` |
| **URL** | `/lab/test-reference/{labId}/delete-all` |
| **Description** | Delete all test references for a lab |
| **Task** | Delete All References |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Require confirmation flag in request body | 0.5 |
| ST-4 | Bulk delete all reference records for lab | 0.5 |
| ST-5 | Return count of deleted records | 0.5 |
| **Total** | | **2** |

---

### Endpoint 35 — Update Reference by ID

| Field | Details |
|-------|---------|
| **Method** | `PUT` |
| **URL** | `/lab/test-reference/{labId}/{testReferenceId}` |
| **Description** | Update a specific test reference (ranges, units, gender, age group) |
| **Task** | Update Reference by ID |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `testReferenceId` | 0.5 |
| ST-3 | Validate updated fields: min, max, unit, age range, gender | 0.5 |
| ST-4 | Ensure min value is less than max value | 0.5 |
| ST-5 | Update reference record | 0.5 |
| ST-6 | Return updated reference data | 0.25 |
| ST-7 | Handle error: reference not found | 0.5 |
| **Total** | | **3** |

---

### Endpoint 36 — Update Reference by Test Code

| Field | Details |
|-------|---------|
| **Method** | `PUT` |
| **URL** | `/lab/test-reference/update` |
| **Description** | Update test reference using test code (alternative to ID-based update) |
| **Task** | Update Reference by Code |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `testCode` and `labId` in request body | 0.5 |
| ST-3 | Lookup reference record by test code + labId | 0.5 |
| ST-4 | Validate updated range values | 0.5 |
| ST-5 | Update and persist record | 0.5 |
| ST-6 | Return updated reference | 0.25 |
| ST-7 | Handle error: test code not found in lab | 0.5 |
| **Total** | | **3** |

---

### Endpoint 37 — Get Reference by Test Name

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/test-reference/{labId}/test/{testName}` |
| **Description** | Get reference data for a specific test name |
| **Task** | Get Reference by Name |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `testName` path param | 0.5 |
| ST-3 | Query reference by test name (case-insensitive match) | 0.75 |
| ST-4 | Return list of references (may have multiple: male/female/age groups) | 0.5 |
| **Total** | | **2** |

---

### Endpoint 38 — Search Test References

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/test-reference/{labId}/test` |
| **Description** | Search test references by keyword (test name or code) |
| **Task** | Search References |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `search` query param | 0.25 |
| ST-3 | Query: test name or code LIKE %keyword% filtered by labId | 0.75 |
| ST-4 | Return matching reference records | 0.75 |
| **Total** | | **2** |

---

### Endpoint 39 — Download References as CSV

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/test-reference/{labId}/download` |
| **Description** | Export test references as a downloadable CSV file |
| **Task** | Download References CSV |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Fetch all reference records for lab | 0.5 |
| ST-4 | Map fields to CSV columns (test code, name, min, max, unit, gender, age range) | 0.75 |
| ST-5 | Write to CSV stream with headers | 0.5 |
| ST-6 | Set Content-Disposition for download | 0.25 |
| ST-7 | Return CSV file stream | 0.5 |
| **Total** | | **3** |

**Phase 4B Total Hours: 26**

---

## Phase 5 — Result Entry & Report Generation

**Controller:** `ReportGeneration.java`
**Base Path:** `/lab/{labId}`

---

### Endpoint 40 — Create Report / Enter Test Results

| Field | Details |
|-------|---------|
| **Method** | `POST` |
| **URL** | `/lab/{labId}/report` |
| **Description** | Enter test results for a visit; creates report records per test |
| **Task** | Create Report / Enter Results |
| **Total Hours** | 8 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `visitId` in request body | 0.5 |
| ST-3 | Validate visit status is COLLECTED (cannot enter results for PENDING visits) | 0.5 |
| ST-4 | Validate result list: each entry must have testId and resultValue | 0.75 |
| ST-5 | For each test: fetch reference ranges based on patient gender and age | 1 |
| ST-6 | Compare result value against reference range; flag as HIGH/LOW/NORMAL | 1 |
| ST-7 | Create a report record per test with: value, unit, status (H/L/N), remarks | 1 |
| ST-8 | Link all reports to the visit | 0.5 |
| ST-9 | Set report status to DRAFT (not yet finalized) | 0.5 |
| ST-10 | Persist all report records in one transaction | 0.5 |
| ST-11 | Return list of created report records | 0.5 |
| ST-12 | Handle error: test not in visit's test list | 0.5 |
| ST-13 | Handle error: missing result for mandatory test | 0.5 |
| **Total** | | **8** |

---

### Endpoint 41 — Get Reports by Visit

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/report/{visitId}` |
| **Description** | Retrieve all reports/results for a specific visit |
| **Task** | Get Reports by Visit |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `visitId` | 0.5 |
| ST-3 | Fetch all report records linked to the visit | 0.75 |
| ST-4 | For each report: include test name, result, unit, reference range, flag | 0.75 |
| ST-5 | Include patient and visit metadata in response | 0.5 |
| ST-6 | Return structured report response ready for display/print | 0.25 |
| **Total** | | **3** |

---

### Endpoint 42 — Bulk Update Reports

| Field | Details |
|-------|---------|
| **Method** | `PUT` |
| **URL** | `/lab/{labId}/report` |
| **Description** | Update multiple report records in bulk (re-enter or correct results) |
| **Task** | Bulk Update Reports |
| **Total Hours** | 5 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Validate list of report update objects (reportId + new value) | 0.75 |
| ST-4 | Check visit is not COMPLETED (cannot update finalized reports) | 0.5 |
| ST-5 | For each report: re-evaluate H/L/N flag against reference range | 1 |
| ST-6 | Update result value, flag, and remarks for each report | 1 |
| ST-7 | Persist all updates in one transaction | 0.5 |
| ST-8 | Return list of updated report records | 0.25 |
| ST-9 | Handle error: reportId not found or belongs to different lab | 0.5 |
| **Total** | | **5** |

---

### Endpoint 43 — Get Report by ID

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/report/by-id/{reportId}` |
| **Description** | Get a single specific report record |
| **Task** | Get Report by ID |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `reportId` | 0.5 |
| ST-3 | Fetch report with test name, result, reference range, patient context | 0.75 |
| ST-4 | Return full report detail | 0.5 |
| **Total** | | **2** |

---

### Endpoint 44 — Update Report by ID

| Field | Details |
|-------|---------|
| **Method** | `PUT` |
| **URL** | `/lab/{labId}/report/by-id/{reportId}` |
| **Description** | Update a single report record by its ID |
| **Task** | Update Report by ID |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `reportId` ownership | 0.5 |
| ST-3 | Validate new result value and remarks | 0.5 |
| ST-4 | Check parent visit is not COMPLETED | 0.5 |
| ST-5 | Re-evaluate H/L/N flag based on new value | 0.5 |
| ST-6 | Update and persist | 0.25 |
| ST-7 | Return updated report record | 0.5 |
| **Total** | | **3** |

---

### Endpoint 45 — Complete Visit (Finalize Reports)

| Field | Details |
|-------|---------|
| **Method** | `PUT` |
| **URL** | `/lab/{labId}/complete-visit/{visitId}` |
| **Description** | Mark a visit as Complete; finalizes all test results and locks reports |
| **Task** | Complete Visit |
| **Total Hours** | 6 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `visitId` | 0.5 |
| ST-3 | Check visit status is COLLECTED (must not be PENDING or already COMPLETED) | 0.5 |
| ST-4 | Validate all tests in the visit have a report entry (no missing results) | 1 |
| ST-5 | Change all report statuses from DRAFT → FINALIZED | 0.5 |
| ST-6 | Update visit status from COLLECTED → COMPLETED | 0.5 |
| ST-7 | Record completion timestamp and completed-by user | 0.5 |
| ST-8 | Lock reports (prevent further edits) | 0.5 |
| ST-9 | Trigger notification / mark report ready for delivery (if applicable) | 0.5 |
| ST-10 | Return completed visit summary | 0.5 |
| ST-11 | Handle error: missing results for some tests | 0.25 |
| ST-12 | Handle error: visit already completed | 0.5 |
| **Total** | | **6** |

---

### Endpoint 46 — Cancel Visit and Reports

| Field | Details |
|-------|---------|
| **Method** | `PUT` |
| **URL** | `/lab/{labId}/cancled-visit/{visitId}` |
| **Description** | Cancel a visit and void all associated reports |
| **Task** | Cancel Visit & Reports |
| **Total Hours** | 4 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `visitId` | 0.5 |
| ST-3 | Check visit is not already COMPLETED (cannot cancel completed visits) | 0.5 |
| ST-4 | Update all linked reports status to VOIDED | 0.75 |
| ST-5 | Update visit status to CANCELLED | 0.5 |
| ST-6 | Update linked billing status to CANCELLED | 0.5 |
| ST-7 | Record cancellation reason and timestamp | 0.5 |
| ST-8 | Return cancellation confirmation | 0.5 |
| **Total** | | **4** |

**Phase 5 Total Hours: 31**

---

## Phase 6 — Report Settings / Template

**Controller:** `ReportSettingController.java`
**Base Path:** `/lab/{labId}/report-settings`

---

### Endpoint 47 — Get Report Settings

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/report-settings` |
| **Description** | Get current report template configuration for a lab |
| **Task** | Get Report Settings |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Fetch report settings record for lab | 0.75 |
| ST-4 | Return settings: header text, logo URL, typography, disclaimer, signature URLs | 0.75 |
| **Total** | | **2** |

---

### Endpoint 48 — Create Report Settings

| Field | Details |
|-------|---------|
| **Method** | `POST` |
| **URL** | `/lab/{labId}/report-settings` |
| **Description** | Create report template settings for a lab (header, logo, signatures, disclaimer, typography) |
| **Task** | Create Report Settings |
| **Total Hours** | 6 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Check no settings already exist for lab (use PUT to update) | 0.5 |
| ST-4 | Validate header fields: lab name, address, phone, email | 0.5 |
| ST-5 | Validate typography settings: font size, font family | 0.5 |
| ST-6 | Validate disclaimer text (not null if required) | 0.5 |
| ST-7 | Validate signature settings: number of signatures, labels | 0.5 |
| ST-8 | Save report settings record | 0.75 |
| ST-9 | Associate logo and signature image S3 keys if provided | 0.75 |
| ST-10 | Return created settings record | 0.5 |
| ST-11 | Handle error: settings already exist | 0.5 |
| ST-12 | Handle error: invalid font or layout values | 0.5 |
| **Total** | | **6** |

---

### Endpoint 49 — Update Report Settings

| Field | Details |
|-------|---------|
| **Method** | `PUT` |
| **URL** | `/lab/{labId}/report-settings` |
| **Description** | Update existing report template configuration |
| **Task** | Update Report Settings |
| **Total Hours** | 4 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Fetch existing settings (404 if not found) | 0.5 |
| ST-4 | Validate incoming update fields | 0.5 |
| ST-5 | Apply partial update (only update provided fields) | 0.75 |
| ST-6 | Update S3 keys for logo/signatures if new ones provided | 0.75 |
| ST-7 | Persist and return updated settings | 0.5 |
| ST-8 | Handle error: settings not found | 0.5 |
| **Total** | | **4** |

---

### Endpoint 50 — Get Pre-signed URL for Signature Upload

| Field | Details |
|-------|---------|
| **Method** | `POST` |
| **URL** | `/lab/{labId}/report-settings/signature/upload-url` |
| **Description** | Generate a pre-signed S3 URL for uploading a signature/logo image |
| **Task** | Get Signature Upload URL |
| **Total Hours** | 4 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Validate request: file name, content type (image/png, image/jpeg only) | 0.5 |
| ST-4 | Generate unique S3 object key (e.g., lab/{labId}/signatures/{uuid}.png) | 0.5 |
| ST-5 | Call AWS S3 SDK to generate pre-signed PUT URL with expiry (e.g., 15 min) | 1 |
| ST-6 | Return pre-signed URL and S3 key to client | 0.5 |
| ST-7 | Client uploads directly to S3 using this URL (not via server) | 0 |
| ST-8 | Store S3 key in report settings after client confirms upload | 0.5 |
| ST-9 | Handle error: unsupported file type | 0.5 |
| **Total** | | **4** |

**Phase 6 Total Hours: 16**

---

## Phase 7 — Billing & Payments

**Controllers:** `BillingController.java`, `PatientController.java`, `StaticController.java`

---

### Endpoint 51 — Get Billing Records

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/billing` |
| **Description** | Retrieve all billing records for a lab with optional filters |
| **Task** | Get Billing Records |
| **Total Hours** | 4 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Accept optional query filters: date range, payment status (PAID/UNPAID/PARTIAL) | 0.5 |
| ST-4 | Query billing records with filters applied | 0.75 |
| ST-5 | Join with patient and visit to include patient name, visit date | 0.75 |
| ST-6 | Return paginated list with total billed, paid, and balance per record | 0.75 |
| ST-7 | Handle empty result | 0.25 |
| ST-8 | Handle invalid filter values gracefully | 0.5 |
| **Total** | | **4** |

---

### Endpoint 52 — Add Partial/Full Payment

| Field | Details |
|-------|---------|
| **Method** | `POST` |
| **URL** | `/lab/{labId}/billing/{billingId}/partial-payment` |
| **Description** | Record a partial or full payment against a billing record |
| **Task** | Add Payment |
| **Total Hours** | 5 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `billingId` ownership | 0.5 |
| ST-3 | Validate payment amount (must be > 0 and ≤ remaining balance) | 0.75 |
| ST-4 | Validate payment mode: CASH, CARD, UPI, etc. | 0.5 |
| ST-5 | Record payment entry with amount, mode, timestamp, received-by | 0.75 |
| ST-6 | Update billing: add to total_paid, recalculate balance | 0.75 |
| ST-7 | If balance = 0: update billing status to PAID | 0.5 |
| ST-8 | If balance > 0: set status to PARTIAL | 0.25 |
| ST-9 | Return updated billing record with payment history | 0.5 |
| ST-10 | Handle error: payment exceeds balance | 0.25 |
| **Total** | | **5** |

---

### Endpoint 53 — Transaction Details by Date

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/statistics/{labId}/datewise-transactionsdetails` |
| **Description** | Get all transaction details (paginated) by date range |
| **Task** | Transaction Details |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId`, `startDate`, `endDate` | 0.5 |
| ST-3 | Query all billing and payment records in date range | 0.75 |
| ST-4 | Include: patient name, amount billed, amount paid, balance, payment mode | 0.75 |
| ST-5 | Return paginated list | 0.5 |
| ST-6 | Handle invalid date range | 0.25 |
| **Total** | | **3** |

---

### Endpoint 54 — Payment Details by Date

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/statistics/{labId}/datewise-paymentdetails` |
| **Description** | Get payment details for bills paid within a date range |
| **Task** | Payment Details |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId`, `startDate`, `endDate` | 0.5 |
| ST-3 | Query payment entries WHERE payment_date BETWEEN startDate AND endDate | 0.75 |
| ST-4 | Aggregate per day: total collected, cash, card, UPI | 0.75 |
| ST-5 | Return paginated response with daily breakdown | 0.5 |
| ST-6 | Handle empty results | 0.25 |
| **Total** | | **3** |

---

### Endpoint 55 — Lab Statistics Summary

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/statistics/{labId}` |
| **Description** | Get overall lab statistics and financial summary by date range |
| **Task** | Lab Statistics |
| **Total Hours** | 5 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId`, `startDate`, `endDate` | 0.5 |
| ST-3 | Count: total patients registered in range | 0.5 |
| ST-4 | Count: total visits created in range | 0.5 |
| ST-5 | Count: total tests conducted | 0.5 |
| ST-6 | Sum: total revenue billed | 0.5 |
| ST-7 | Sum: total revenue collected (paid) | 0.5 |
| ST-8 | Sum: total outstanding balance | 0.5 |
| ST-9 | Return aggregated statistics object | 0.5 |
| ST-10 | Handle error: invalid date range | 0.25 |
| **Total** | | **5** |

**Phase 7 Total Hours: 20**

---

## Phase 8 — Sample Type Configuration

**Controller:** `SampleAssociation.java`
**Base Path:** `/lab/{labId}`

---

### Endpoint 56 — List Sample Types

| Field | Details |
|-------|---------|
| **Method** | `GET` |
| **URL** | `/lab/{labId}/sample-list` |
| **Description** | Get all sample types configured in the lab |
| **Task** | List Sample Types |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Query sample_type table by labId | 0.75 |
| ST-4 | Return list (id, name, description) | 0.75 |
| **Total** | | **2** |

---

### Endpoint 57 — Create Sample Type

| Field | Details |
|-------|---------|
| **Method** | `POST` |
| **URL** | `/lab/{labId}/sample` |
| **Description** | Create a new sample type (e.g., Blood, Urine, Serum) |
| **Task** | Create Sample Type |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` | 0.25 |
| ST-3 | Validate `name` is present and not empty | 0.5 |
| ST-4 | Check for duplicate sample type name in same lab | 0.5 |
| ST-5 | Save new sample type | 0.5 |
| ST-6 | Return created sample type record | 0.5 |
| ST-7 | Handle error: duplicate name | 0.5 |
| **Total** | | **3** |

---

### Endpoint 58 — Update Sample Type

| Field | Details |
|-------|---------|
| **Method** | `PUT` |
| **URL** | `/lab/{labId}/sample/{sampleId}` |
| **Description** | Update an existing sample type |
| **Task** | Update Sample Type |
| **Total Hours** | 2 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `sampleId` | 0.5 |
| ST-3 | Validate updated name | 0.25 |
| ST-4 | Check for duplicate name (excluding current record) | 0.5 |
| ST-5 | Update and return sample type | 0.5 |
| **Total** | | **2** |

---

### Endpoint 59 — Delete Sample Type

| Field | Details |
|-------|---------|
| **Method** | `DELETE` |
| **URL** | `/lab/{labId}/sample/{sampleId}` |
| **Description** | Delete a sample type from the lab |
| **Task** | Delete Sample Type |
| **Total Hours** | 3 |

| # | Subtask | Hours |
|---|---------|-------|
| ST-1 | Authenticate JWT token | 0.25 |
| ST-2 | Validate `labId` and `sampleId` | 0.5 |
| ST-3 | Check if sample type is currently linked to any visit/collection (block if yes) | 1 |
| ST-4 | Delete sample type record | 0.25 |
| ST-5 | Return success or block message | 0.5 |
| ST-6 | Handle error: sample type in use | 0.5 |
| **Total** | | **3** |

**Phase 8 Total Hours: 10**

---

## Hour Estimate Summary

| Phase | Area | Endpoints | Total Hours |
|-------|------|-----------|-------------|
| Phase 1 | Patient Management | 8 | 30 |
| Phase 2 | Visit Management | 11 | 37 |
| Phase 3 | Sample Collection | 4 | 14 |
| Phase 4A | Test Management | 8 | 29 |
| Phase 4B | Test Reference Values | 8 | 26 |
| Phase 5 | Result Entry & Report Generation | 7 | 31 |
| Phase 6 | Report Settings / Template | 4 | 16 |
| Phase 7 | Billing & Payments | 5 | 20 |
| Phase 8 | Sample Type Configuration | 4 | 10 |
| **Grand Total** | | **59** | **213** |

---

## Subtask Category Breakdown

| Subtask Type | Description | Approx. Hours Each |
|---|---|---|
| Authentication | JWT token validation on every endpoint | 0.25 |
| Ownership Validation | Confirm entity belongs to the lab | 0.25 – 0.5 |
| Input Validation | Null checks, type checks, business rules | 0.25 – 0.75 |
| Business Logic | Status transitions, flag evaluation, auto-calculations | 0.5 – 1.5 |
| Database Query | SELECT / INSERT / UPDATE / DELETE with joins | 0.5 – 1 |
| Cascade Operations | Multi-entity deletes or updates in transaction | 0.5 – 1 |
| File Processing | CSV parse, validate, bulk insert, stream download | 0.5 – 1.5 |
| External Integration | AWS S3 pre-signed URL generation | 0.5 – 1 |
| Response Mapping | Entity to DTO mapping and return | 0.25 – 0.5 |
| Error Handling | 404, 400, conflict responses | 0.25 – 0.5 |

---

*Document generated: 2026-06-29*
*Total Endpoints: 59 | Total Subtasks: 350+ | Grand Total Estimated Hours: 213*
